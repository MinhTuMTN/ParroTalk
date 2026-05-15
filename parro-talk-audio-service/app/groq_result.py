import re


def to_dict(obj):
    if isinstance(obj, dict):
        return obj
    if hasattr(obj, "model_dump"):
        return obj.model_dump()
    if hasattr(obj, "dict"):
        return obj.dict()
    if hasattr(obj, "__dict__"):
        return obj.__dict__
    return {}


def _normalize_word_token(token: str) -> str:
    if not isinstance(token, str):
        return ""
    return re.sub(r"\s+", " ", token).strip().lower().strip("\"'.,!?;:()[]{}")


def filter_words_in_segment(words: list, seg_start: float, seg_end: float, seg: dict) -> list:
    seg_text = seg.get("text", "") if isinstance(seg, dict) else ""
    seg_tokens = [_normalize_word_token(token) for token in seg_text.split()]
    seg_tokens = [token for token in seg_tokens if token]

    words_in_window = [word for word in words if word["start"] < seg_end and word["end"] > seg_start]
    if not seg_tokens:
        return words_in_window

    result = []
    token_idx = 0

    for word in words_in_window:
        word_text = _normalize_word_token(word.get("word", ""))
        if not word_text:
            continue

        while token_idx < len(seg_tokens) and seg_tokens[token_idx] != word_text:
            token_idx += 1

        if token_idx >= len(seg_tokens):
            break

        result.append(word)
        token_idx += 1

    return result
