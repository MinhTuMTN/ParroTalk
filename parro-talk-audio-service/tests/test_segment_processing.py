import unittest

from app.segment_processing import build_canonical_segments
from app.chunk_processing import merge_chunk_results


def make_words(text):
    words = []
    current = 0.0
    for token in text.split():
        words.append({"word": token, "start": current, "end": current + 0.2})
        current += 0.25
    return words


class CanonicalSegmentTests(unittest.TestCase):
    def test_splits_terminal_punctuation_and_preserves_text(self):
        text = (
            "This opening sentence already has enough words to stand alone. "
            "Are you ready to continue with the next complete sentence? "
            "Yes, this final sentence also has enough words."
        )
        segments = build_canonical_segments(text, make_words(text))

        self.assertEqual([segment["text"] for segment in segments], [
            "This opening sentence already has enough words to stand alone.",
            "Are you ready to continue with the next complete sentence?",
            "Yes, this final sentence also has enough words.",
        ])

    def test_splits_long_sentence_once_at_comma(self):
        text = (
            "One two three four five six seven eight nine ten eleven, "
            "twelve thirteen fourteen fifteen sixteen seventeen eighteen nineteen twenty one."
        )
        segments = build_canonical_segments(text, make_words(text))

        self.assertEqual(len(segments), 2)
        self.assertTrue(segments[0]["text"].endswith("eleven,"))

    def test_splits_long_sentence_once_at_conjunction(self):
        text = (
            "One two three four five six seven eight nine ten eleven twelve "
            "and thirteen fourteen fifteen sixteen seventeen eighteen nineteen twenty one."
        )
        segments = build_canonical_segments(text, make_words(text))

        self.assertEqual(len(segments), 2)
        self.assertTrue(segments[1]["text"].startswith("and "))

    def test_keeps_canonical_text_when_words_have_minor_mismatch(self):
        text = "Maybe you are sitting by the window."
        words = make_words("Maybe you sitting by the window.")
        segments = build_canonical_segments(text, words)

        self.assertEqual(segments[0]["text"], text)
        self.assertEqual(segments[0]["start"], words[0]["start"])
        self.assertEqual(segments[0]["end"], words[-1]["end"])

    def test_timestamps_increase_across_segments(self):
        text = (
            "First sentence has enough words to remain a standalone segment. "
            "Second sentence also has enough words to remain by itself."
        )
        segments = build_canonical_segments(text, make_words(text))

        self.assertLessEqual(segments[0]["end"], segments[1]["start"])

    def test_merges_short_sentence_with_next_segment(self):
        text = (
            "Tiny sentence. "
            "This following sentence is long enough to absorb the short one."
        )
        segments = build_canonical_segments(text, make_words(text))

        self.assertEqual(len(segments), 1)
        self.assertEqual(
            segments[0]["text"],
            "Tiny sentence. This following sentence is long enough to absorb the short one.",
        )

    def test_merges_short_final_sentence_with_previous_segment(self):
        text = (
            "This opening sentence already has enough words to stand alone. "
            "Tiny ending."
        )
        segments = build_canonical_segments(text, make_words(text))

        self.assertEqual(len(segments), 1)
        self.assertTrue(segments[0]["text"].endswith("Tiny ending."))

    def test_merges_chunk_results_before_processing(self):
        first = {
            "text": "First sentence.",
            "segments": [{"start": 0.0, "end": 1.0, "text": "First sentence."}],
            "words": [
                {"word": "First", "start": 0.0, "end": 0.4},
                {"word": "sentence.", "start": 0.4, "end": 1.0},
            ],
        }
        second = {
            "text": "Second sentence.",
            "segments": [{"start": 0.0, "end": 1.0, "text": "Second sentence."}],
            "words": [
                {"word": "Second", "start": 0.0, "end": 0.4},
                {"word": "sentence.", "start": 0.4, "end": 1.0},
            ],
        }

        merged = merge_chunk_results([(first, 0), (second, 10)])

        self.assertEqual(merged["text"], "First sentence. Second sentence.")
        self.assertEqual(merged["words"][-1]["end"], 11.0)


if __name__ == "__main__":
    unittest.main()
