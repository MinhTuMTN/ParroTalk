from app.groq_result import to_dict


def adjust_segment_timestamps(segment, chunk_start: int) -> dict:
    if isinstance(segment, dict):
        adjusted_segment = {
            "start": segment["start"] + chunk_start,
            "end": segment["end"] + chunk_start,
            "text": segment["text"],
        }
        raw_words = segment.get("words")
    else:
        adjusted_segment = {
            "start": segment.start + chunk_start,
            "end": segment.end + chunk_start,
            "text": segment.text,
        }
        raw_words = segment.words if hasattr(segment, "words") else None

    if raw_words:
        adjusted_words = []
        for word in raw_words:
            word_dict = to_dict(word)
            adjusted_words.append({
                "word": word_dict["word"],
                "start": word_dict["start"] + chunk_start,
                "end": word_dict["end"] + chunk_start,
            })
        adjusted_segment["words"] = adjusted_words

    return adjusted_segment


def merge_chunk_results(chunk_results: list[tuple[dict, int]]) -> dict:
    all_segments = []
    all_words = []
    full_text_parts = []

    for result_dict, chunk_start in chunk_results:
        full_text_parts.append(result_dict.get("text", ""))

        for raw_word in result_dict.get("words", []):
            word = to_dict(raw_word)
            all_words.append({
                "word": word["word"],
                "start": word["start"] + chunk_start,
                "end": word["end"] + chunk_start,
            })

        for segment in result_dict.get("segments", []):
            all_segments.append(adjust_segment_timestamps(segment, chunk_start))

    return {
        "text": " ".join(part.strip() for part in full_text_parts if part).strip(),
        "segments": all_segments,
        "words": all_words,
    }
