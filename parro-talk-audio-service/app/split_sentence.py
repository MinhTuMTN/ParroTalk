"""
sentence_splitter.py
 
Splits an over-long sentence into shorter segments, each bounded by
MAX_WORDS_PER_SEGMENT, preferring natural break points (hard/soft
punctuation, conjunctions, discourse markers) that sit close to the
word-count target instead of the midpoint.
 
Architecture
------------
- BREAK_CONJUNCTIONS / _MARKER_INDEX  -> configuration + fast phrase lookup
- score_split_candidate                -> scores ONE candidate index
- collect_split_candidates              -> all candidates in a bounded window
- find_best_split_index                 -> argmax over candidates
- split_sentence                        -> single split, string in/out (easy to unit test)
- split_until_valid                     -> production entry point, O(n), iterative
 
Everything below `split_sentence` operates on a *single, shared* token
list plus (start, end) index ranges, rather than re-tokenizing substrings
at every recursive step. That's what keeps the whole pipeline O(n) instead
of O(n^2 / MAX_WORDS_PER_SEGMENT) for very long input.
"""
 
from __future__ import annotations
 
import re
from dataclasses import dataclass
from typing import Optional
 
# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
 
MAX_WORDS_PER_SEGMENT = 20
MIN_SPLIT_SIDE_WORDS = 5
 
# How far (in words) from MAX_WORDS_PER_SEGMENT we're willing to look for a
# split point. Keeps the search local instead of scanning the whole sentence
# (requirement #5) -- e.g. MAX=20, MARGIN=5 => search window is [15, 25].
SEARCH_WINDOW_MARGIN = 5
 
# --- Natural-break weights -------------------------------------------------
# Gaps between tiers (>=10) are deliberately bigger than PROXIMITY_WEIGHT's
# whole range (0-5). That guarantees "a better kind of break" always beats
# "a closer but weaker one" -- proximity only breaks ties *within* a tier,
# or ranks pure fallback positions when nothing else is present.
WEIGHT_HARD_BREAK = 110          # . ! ? in the middle of the sentence
WEIGHT_PUNCTUATION = 100         # , ; : right before the split
WEIGHT_MULTI_WORD_MARKER = 90    # "on the other hand", "as a result", ...
WEIGHT_DISCOURSE_MARKER = 80     # "however", "therefore", "moreover", ...
WEIGHT_SUBORDINATING = 70        # "because"
WEIGHT_CAPITALIZED_BOUNDARY = 65 # heuristic: likely clause start, see below
WEIGHT_COORDINATING = 60         # "and", "or", "but", "so"
PROXIMITY_WEIGHT = 5             # max bonus for landing exactly on target
 
# Transcripts (ASR output, subtitles, ...) often lose sentence-ending
# punctuation but keep capitalization, e.g.:
#   "...if they can see their device Having the device in view..."
# "Having" is almost certainly a lost sentence boundary. This signal is
# rarer and more reliable than a generic conjunction, so -- unlike
# BREAK_CONJUNCTIONS -- it gets its own wider search instead of being
# confined to SEARCH_WINDOW_MARGIN (see _find_boundary_indices). It's
# still just a heuristic (proper nouns are also capitalized), so it's
# a flag you can turn off for text where that trade-off doesn't hold.
ENABLE_SENTENCE_BOUNDARY_HEURISTIC = True
 
# Words that are capitalized *everywhere*, not just at a clause start, so
# they carry no boundary information ("I" is always "I", not just after a
# full stop). Extend this if your data has other always-capitalized tokens.
_ALWAYS_CAPITALIZED_WORDS = {"i", "i'm", "i've", "i'll", "i'd"}
 
BREAK_CONJUNCTIONS = {
    "and", "or", "but", "so", "because", "however", "therefore", "meanwhile",
    "consequently", "furthermore", "moreover", "nevertheless", "nonetheless",
    "otherwise", "still", "thus", "yet", "finally", "eventually", "actually",
    "for example", "for instance", "in addition", "in conclusion", "in summary", "in short",
    "as a result", "as a consequence", "at last", "at first", "at least", "at most",
    "by the way", "in any case", "in any event", "in fact", "in other words", "in the meantime",
    "instead", "on the contrary", "on the other hand",
}
 
# Optional per-marker fine-tuning. Anything in BREAK_CONJUNCTIONS that is
# *not* listed here falls back to a sensible default based on word count
# (see _DEFAULT_WEIGHT_BY_WORD_COUNT) -- so adding a brand-new marker to the
# set above "just works" without also having to edit this dict.
_MARKER_WEIGHT_OVERRIDES: dict[str, int] = {
    "and": WEIGHT_COORDINATING, "or": WEIGHT_COORDINATING,
    "but": WEIGHT_COORDINATING, "so": WEIGHT_COORDINATING,
    "because": WEIGHT_SUBORDINATING,
    "at least": 50, "at most": 50,  # rarely a real clause boundary
}
 
_DEFAULT_WEIGHT_BY_WORD_COUNT = {
    1: WEIGHT_DISCOURSE_MARKER,
    2: WEIGHT_MULTI_WORD_MARKER,
    3: WEIGHT_MULTI_WORD_MARKER,
    4: WEIGHT_MULTI_WORD_MARKER,
    5: WEIGHT_MULTI_WORD_MARKER,
}
 
WEIGHT_BAD_SPLIT = -120

DETERMINERS = {
    "the", "a", "an",
    "this", "that", "these", "those",
    "my", "your", "his", "her", "its",
    "our", "their",
}

PREPOSITIONS = {
    "to", "for", "of", "in", "on", "at",
    "by", "with", "from", "into", "onto",
    "over", "under", "about", "after",
    "before", "between", "through",
}

COMMON_PHRASAL_PARTICLES = {
    "up", "down", "off", "out", "away",
    "back", "over", "around", "apart",
    "together", "through", "along",
}

def _is_capitalized_boundary(tokens: list[str], index: int) -> bool:
    if index >= len(tokens):
        return False

    token = tokens[index].strip("\"'()[]{}")

    if not token:
        return False

    if not token[0].isupper():
        return False

    if _clean_token(token) in _ALWAYS_CAPITALIZED_WORDS:
        return False

    return True

def _bad_split_penalty(tokens: list[str], index: int) -> int:
    """
    Return negative score if splitting at `index` breaks a phrase.
    """

    if index <= 0 or index >= len(tokens):
        return 0

    left = _clean_token(tokens[index - 1])
    right = _clean_token(tokens[index])

    penalty = 0

    #
    # their | screen
    # the | device
    #
    if left in DETERMINERS:
        penalty -= 90

    #
    # for | success
    # in | view
    #
    if left in PREPOSITIONS:
        penalty -= 80

    #
    # to | reduce
    #
    if left == "to":
        penalty -= 90

    #
    # set | up
    # give | up
    #
    if right in COMMON_PHRASAL_PARTICLES:
        penalty -= 80

    #
    # don't split after article then adjective
    #
    if right in {"big", "small", "good", "bad", "new", "old"}:
        penalty -= 20

    return penalty

def _marker_weight(phrase: str) -> int:
    if phrase in _MARKER_WEIGHT_OVERRIDES:
        return _MARKER_WEIGHT_OVERRIDES[phrase]
    word_count = len(phrase.split())
    return _DEFAULT_WEIGHT_BY_WORD_COUNT.get(word_count, WEIGHT_DISCOURSE_MARKER)
 
 
def _clean_token(token: str) -> str:
    return token.lower().strip().strip("\"'.,!?;:()[]{}")
 
 
_HARD_BREAK_RE = re.compile(r"[.!?][\"')\]]*$")
_SOFT_BREAK_RE = re.compile(r"[,;:\u2014\u2013][\"')\]]*$")  # , ; : — –
 
 
def _break_kind(token: str) -> Optional[str]:
    stripped = token.strip()
    if _HARD_BREAK_RE.search(stripped):
        return "hard"
    if _SOFT_BREAK_RE.search(stripped):
        return "soft"
    return None
 
 
@dataclass(frozen=True)
class _MarkerIndex:
    """Precomputed lookup: {phrase length in words -> {phrase tuple: weight}}.
 
    Longest-match-first means "on the other hand" wins over any accidental
    single-word overlap, and lookup is a plain dict hit -- O(1) per length,
    O(max marker length) per position, which is a constant.
    """
    by_length: dict[int, dict[tuple[str, ...], int]]
    max_length: int
 
    @classmethod
    def build(cls, markers: set) -> "_MarkerIndex":
        by_length: dict[int, dict[tuple[str, ...], int]] = {}
        for phrase in markers:
            words = tuple(phrase.lower().split())
            by_length.setdefault(len(words), {})[words] = _marker_weight(phrase)
        max_length = max(by_length) if by_length else 0
        return cls(by_length=by_length, max_length=max_length)
 
    def match_at(self, tokens: list[str], index: int, end: int) -> Optional[tuple[int, int]]:
        """(weight, phrase_length) of the longest marker starting exactly at
        `index`, bounded by `end` (exclusive) so we never read into a
        sibling segment. Returns None if nothing matches."""
        limit = min(self.max_length, end - index)
        for length in range(limit, 0, -1):
            phrase = tuple(_clean_token(t) for t in tokens[index:index + length])
            weight = self.by_length.get(length, {}).get(phrase)
            if weight is not None:
                return weight, length
        return None
 
 
_MARKER_INDEX = _MarkerIndex.build(BREAK_CONJUNCTIONS)
 
 
# ---------------------------------------------------------------------------
# Candidate scoring
# ---------------------------------------------------------------------------
 
@dataclass(frozen=True)
class SplitCandidate:
    index: int      # cut point: left = tokens[start:index], right = tokens[index:end]
    score: float
    reason: str      # human-readable -- handy for debugging and unit tests

def score_split_candidate(
    tokens: list[str],
    index: int,
    end: int,
    target: int,
) -> SplitCandidate:

    score = 0.0
    reasons = []

    #
    # punctuation
    #
    break_kind = _break_kind(tokens[index - 1])

    if break_kind == "hard":
        score += WEIGHT_HARD_BREAK
        reasons.append("hard-break")

    elif break_kind == "soft":
        score += WEIGHT_PUNCTUATION
        reasons.append("punctuation")

    #
    # discourse marker
    #
    marker = _MARKER_INDEX.match_at(tokens, index, end)

    if marker is not None:
        weight, _ = marker
        score += weight
        reasons.append("marker")

    #
    # ASR sentence boundary:
    # "...device Having..."
    #
    if ENABLE_SENTENCE_BOUNDARY_HEURISTIC and _is_capitalized_boundary(tokens, index):
        score += WEIGHT_CAPITALIZED_BOUNDARY
        reasons.append("capitalized")

    #
    # bad linguistic split
    #
    penalty = _bad_split_penalty(tokens, index)

    if penalty:
        score += penalty
        reasons.append("phrase-penalty")

    #
    # proximity
    #
    distance = abs(index - target)

    score += max(0, PROXIMITY_WEIGHT - distance)

    if not reasons:
        reasons.append("fallback")

    return SplitCandidate(
        index=index,
        score=score,
        reason="+".join(reasons),
    )
 
 
def _blocked_interior_positions(tokens: list[str], lo: int, hi: int, end: int) -> set[int]:
    """Positions in [lo, hi] that fall *inside* a multi-word marker (e.g. the
    "a" / "result" gap inside "as a result"). Cutting there would tear a
    marker phrase in half, which is always worse than cutting at its edges
    -- so these positions are never valid candidates, regardless of score.
    """
    blocked: set[int] = set()
    if _MARKER_INDEX.max_length <= 1:
        return blocked
    scan_start = max(0, lo - (_MARKER_INDEX.max_length - 1))
    for p in range(scan_start, hi + 1):
        match = _MARKER_INDEX.match_at(tokens, p, end)
        if match is None:
            continue
        _weight, length = match
        if length > 1:
            for interior in range(p + 1, p + length):
                if lo <= interior <= hi:
                    blocked.add(interior)
    return blocked
 
 
def collect_split_candidates(
    tokens: list[str], start: int, end: int, max_words: int = MAX_WORDS_PER_SEGMENT
) -> list[SplitCandidate]:
    """Every viable split point for tokens[start:end], restricted to a
    window around `start + max_words` (requirement #5). Falls back to the
    full valid range if the window is empty or every position in it is
    blocked, so a split point is always findable when one exists at all
    (requirement #6)."""
    target = start + max_words
    window = SEARCH_WINDOW_MARGIN

    if ENABLE_SENTENCE_BOUNDARY_HEURISTIC:
        window += 5

    lo = max(start + MIN_SPLIT_SIDE_WORDS, target - window)
    hi = min(end - MIN_SPLIT_SIDE_WORDS, target + window)
    if lo > hi:
        lo, hi = start + MIN_SPLIT_SIDE_WORDS, end - MIN_SPLIT_SIDE_WORDS
    if lo > hi:
        return []  # segment too short to split at all
 
    blocked = _blocked_interior_positions(tokens, lo, hi, end)
    candidates = [
        score_split_candidate(tokens, i, end, target) for i in range(lo, hi + 1) if i not in blocked
    ]
    if candidates:
        return candidates
 
    # Whole window sat inside one marker phrase (rare) -- widen to the full
    # valid range as a last resort rather than reporting "no split found".
    lo, hi = start + MIN_SPLIT_SIDE_WORDS, end - MIN_SPLIT_SIDE_WORDS
    if lo > hi:
        return []
    blocked = _blocked_interior_positions(tokens, lo, hi, end)
    return [
        score_split_candidate(tokens, i, end, target) for i in range(lo, hi + 1) if i not in blocked
    ]
 
 
def find_best_split_index(
    tokens: list[str], start: int, end: int, max_words: int = MAX_WORDS_PER_SEGMENT
) -> Optional[int]:
    candidates = collect_split_candidates(tokens, start, end, max_words)
    if not candidates:
        return None
    return max(candidates, key=lambda c: c.score).index
 
 
# ---------------------------------------------------------------------------
# Splitting
# ---------------------------------------------------------------------------
 
def _split_once(
    tokens: list[str], start: int, end: int, max_words: int
) -> list[tuple[int, int]]:
    if end - start <= max_words:
        return [(start, end)]
    index = find_best_split_index(tokens, start, end, max_words)
    if index is None:
        return [(start, end)]  # can't split further (too few words left)
    return [(start, index), (index, end)]
 
 
def split_sentence(sentence: str, max_words: int = MAX_WORDS_PER_SEGMENT) -> list[str]:
    """Split a sentence ONCE at the single best point. Mainly here for easy
    unit testing / inspection; `split_until_valid` is the real entry point."""
    tokens = sentence.split()
    ranges = _split_once(tokens, 0, len(tokens), max_words)
    if len(ranges) == 1:
        return [sentence]
    return [" ".join(tokens[s:e]).strip() for s, e in ranges]
 
 
def split_until_valid(sentence: str, max_words: int = MAX_WORDS_PER_SEGMENT) -> list[str]:
    """Split repeatedly until every piece is <= max_words, or a piece
    genuinely can't be split further (too few words left).
 
    Iterative (explicit stack) rather than recursive so a pathologically
    long single sentence can't blow Python's recursion limit, and tokenizes
    the sentence exactly once -- overall O(n).
    """
    tokens = sentence.split()
    if not tokens:
        return []
 
    result: list[str] = []
    stack: list[tuple[int, int]] = [(0, len(tokens))]
    while stack:
        start, end = stack.pop()
        ranges = _split_once(tokens, start, end, max_words)
        if len(ranges) == 1:
            result.append(" ".join(tokens[start:end]).strip())
            continue
        left, right = ranges
        stack.append(right)
        stack.append(left)  # pushed last -> popped first -> keeps left-to-right order
 
    return result