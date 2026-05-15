import re

from app.logging_config import logger


MAX_WORDS_PER_SEGMENT = 20
MIN_WORDS_PER_SEGMENT = 8
MIN_SPLIT_SIDE_WORDS = 5
BREAK_CONJUNCTIONS = {"and", "or", "but", "so", "because"}
SENTENCE_RE = re.compile(r".+?(?:[.!?](?:[\"')\]]+)?(?=\s|$)|$)", re.DOTALL)


def build_canonical_segments(text: str, words: list, max_words: int = MAX_WORDS_PER_SEGMENT) -> list:
    sentence_texts = _split_text_into_sentences(text)
    segment_texts = []

    for sentence in sentence_texts:
        segment_texts.extend(_split_long_sentence_once(sentence, max_words))
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

    for token in segment_tokens:
        if not token:
            continue

        match_index = _find_next_token(token, normalized_words, search_from)
        if match_index is None:
            continue

        matched_indexes.append(match_index)
        search_from = match_index + 1

    return matched_indexes


def _find_next_token(token: str, normalized_words: list, start_index: int) -> int | None:
    for index in range(start_index, len(normalized_words)):
        if normalized_words[index] == token:
            return index
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
