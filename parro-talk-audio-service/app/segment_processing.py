from dataclasses import dataclass
from typing import Optional
import re
from app.split_sentence import split_until_valid
from app.logging_config import logger


MAX_WORDS_PER_SEGMENT = 20
MIN_WORDS_PER_SEGMENT = 8
MIN_SPLIT_SIDE_WORDS = 5
BREAK_CONJUNCTIONS = {
    "and", "or", "but", "so", "because", "however", "therefore", "meanwhile",
    "consequently", "furthermore", "moreover", "nevertheless", "nonetheless",
    "otherwise", "still", "thus", "yet", "finally", "eventually", "actually",
    "for example", "for instance", "in addition", "in conclusion", "in summary", "in short",
    "as a result", "as a consequence", "at last", "at first", "at least", "at most",
    "by the way", "in any case", "in any event", "in fact", "in other words", "in the meantime",
    "instead", "on the contrary", "on the other hand",
}
# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

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
WEIGHT_COORDINATING = 60         # "and", "or", "but", "so"
PROXIMITY_WEIGHT = 5             # max bonus for landing exactly on target

SENTENCE_RE = re.compile(r".+?(?:[.!?](?:[\"')\]]+)?(?=\s|$)|$)", re.DOTALL)

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

@dataclass(frozen=True)
class SplitCandidate:
    index: int      # cut point: left = tokens[start:index], right = tokens[index:end]
    score: float
    reason: str      # human-readable -- handy for debugging and unit tests

def score_split_candidate(
    tokens: list[str], index: int, end: int, target: int
) -> SplitCandidate:
    """Score how good it is to cut right before position `index`.
 
    Signals are additive, not exclusive: a comma immediately followed by a
    discourse marker (", however,") scores for *both*, correctly ranking
    above either signal alone.
    """
    score = 0.0
    reasons: list[str] = []
 
    break_kind = _break_kind(tokens[index - 1])
    if break_kind == "hard":
        score += WEIGHT_HARD_BREAK
        reasons.append("hard-break")
    elif break_kind == "soft":
        score += WEIGHT_PUNCTUATION
        reasons.append("punctuation")
 
    marker = _MARKER_INDEX.match_at(tokens, index, end)
    if marker is not None:
        weight, _length = marker
        score += weight
        reasons.append("marker")
 
    distance = abs(index - target)
    score += max(0, PROXIMITY_WEIGHT - distance)
 
    if not reasons:
        reasons.append("fallback-position")
 
    return SplitCandidate(index=index, score=score, reason="+".join(reasons))
 
 
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

def build_canonical_segments(text: str, words: list, max_words: int = MAX_WORDS_PER_SEGMENT) -> list:
    sentence_texts = _split_text_into_sentences(text)
    segment_texts = []

    for sentence in sentence_texts:
        if "I wasn" in sentence:
            logger.debug("Debug")
        segment_texts.extend(split_until_valid(sentence, max_words))
    segment_texts = _merge_short_segments(segment_texts, max_words)

    normalized_words = [_normalize_word(word) for word in words]
    segments = []
    cursor = 0

    for segment_text in segment_texts:
        segment_tokens = _tokenize(segment_text)
        word_indexes = _match_segment_words(segment_tokens, normalized_words, cursor)
        if not word_indexes:
            word_indexes = _fallback_word_indexes(words, cursor, len(segment_tokens))
            logger.warning("Falling back to nearest word timestamps for segment: %s", segment_text)

        matched_words = [words[index].copy() for index in range(word_indexes[0], word_indexes[-1] + 1)]

        segments.append({
            "start": matched_words[0]["start"],
            "end": matched_words[-1]["end"],
            "text": segment_text,
            "words": matched_words,
        })
        cursor = word_indexes[-1] + 1

    logger.info("Built %s canonical segments from transcript text.", len(segments))
    return segments


def _split_text_into_sentences(text: str) -> list:
    if not text:
        return []

    sentences = []
    for match in SENTENCE_RE.finditer(text.strip()):
        sentence = re.sub(r"\s+", " ", match.group(0)).strip()
        if sentence:
            sentences.append(sentence)
    return sentences


def _split_long_sentence_once(sentence: str, max_words: int) -> list:
    parts = sentence.split()
    if len(parts) <= max_words:
        return [sentence]

    split_index = _choose_text_split_index(parts)
    if split_index is None:
        return [sentence]

    return [
        " ".join(parts[:split_index]).strip(),
        " ".join(parts[split_index:]).strip(),
    ]


def _merge_short_segments(segment_texts: list, max_words: int) -> list:
    if not segment_texts:
        return []

    merged = []
    index = 0

    while index < len(segment_texts):
        current = segment_texts[index]
        current_words = len(current.split())

        if current_words < MIN_WORDS_PER_SEGMENT and index + 1 < len(segment_texts):
            next_text = segment_texts[index + 1]
            if current_words + len(next_text.split()) <= max_words:
                merged.append(f"{current} {next_text}")
                index += 2
                continue

        if current_words < MIN_WORDS_PER_SEGMENT and merged:
            previous = merged[-1]
            if len(previous.split()) + current_words <= max_words:
                merged[-1] = f"{previous} {current}"
                index += 1
                continue

        merged.append(current)
        index += 1

    return merged


def _choose_text_split_index(parts: list) -> int | None:
    if len(parts) < MIN_SPLIT_SIDE_WORDS * 2:
        return None

    midpoint = len(parts) / 2
    candidates = []

    for index in range(MIN_SPLIT_SIDE_WORDS, len(parts) - MIN_SPLIT_SIDE_WORDS + 1):
        prev_token = parts[index - 1]
        next_token = parts[index] if index < len(parts) else ""

        if _has_soft_break(prev_token):
            candidates.append((100, index))
        elif _clean_token(next_token) in BREAK_CONJUNCTIONS:
            candidates.append((80, index))
        elif _clean_token(prev_token) in BREAK_CONJUNCTIONS:
            candidates.append((70, index))

    if not candidates:
        return None

    candidates.sort(key=lambda item: (-item[0], abs(item[1] - midpoint)))
    return candidates[0][1]


def _match_segment_words(segment_tokens: list, normalized_words: list, cursor: int) -> list:
    matched_indexes = []
    search_from = cursor

    i = 0
    while i < len(segment_tokens):
        token = segment_tokens[i]

        result = _find_next_token(token, normalized_words, search_from)
        if result is None:
            break

        index, matched_word = result
        matched_indexes.append(index)

        consumed = len(matched_word.split())

        i += consumed
        search_from = index + 1

    return matched_indexes


def _find_next_token(token: str, normalized_words: list, start_index: int) -> tuple[int, dict] | None:
    for index in range(start_index, len(normalized_words)):
        if str(normalized_words[index]).startswith(token):
            return index, normalized_words[index]
    return None


def _fallback_word_indexes(words: list, cursor: int, token_count: int) -> list:
    if not words:
        return []

    if cursor >= len(words):
        return [len(words) - 1]

    span = max(1, token_count)
    end_index = min(len(words) - 1, cursor + span - 1)
    return list(range(cursor, end_index + 1))


def _tokenize(text: str) -> list:
    return [_clean_token(token) for token in text.split() if _clean_token(token)]


def _normalize_word(word: dict) -> str:
    return _clean_token(str(word.get("word", "")))


def _clean_token(token: str) -> str:
    return token.lower().strip().strip("\"'.,!?;:()[]{}")


def _has_soft_break(token: str) -> bool:
    return bool(re.search(r"[,;:][\"')\]]*$", token.strip()))


def _split_once(
    tokens: list[str], start: int, end: int, max_words: int
) -> list[tuple[int, int]]:
    if end - start <= max_words:
        return [(start, end)]
    index = find_best_split_index(tokens, start, end, max_words)
    if index is None:
        return [(start, end)]  # can't split further (too few words left)
    return [(start, index), (index, end)]

def find_best_split_index(
    tokens: list[str], start: int, end: int, max_words: int = MAX_WORDS_PER_SEGMENT
) -> Optional[int]:
    candidates = collect_split_candidates(tokens, start, end, max_words)
    if not candidates:
        return None
    return max(candidates, key=lambda c: c.score).index

def collect_split_candidates(
    tokens: list[str], start: int, end: int, max_words: int = MAX_WORDS_PER_SEGMENT
) -> list[SplitCandidate]:
    """Every viable split point for tokens[start:end], restricted to a
    window around `start + max_words` (requirement #5). Falls back to the
    full valid range if the window is empty or every position in it is
    blocked, so a split point is always findable when one exists at all
    (requirement #6)."""
    target = start + max_words
    lo = max(start + MIN_SPLIT_SIDE_WORDS, target - SEARCH_WINDOW_MARGIN)
    hi = min(end - MIN_SPLIT_SIDE_WORDS, target + SEARCH_WINDOW_MARGIN)
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