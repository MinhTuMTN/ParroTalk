import unittest

from scripts.backfill_edge_tts import (
    PronunciationTarget,
    build_unique_synthesis_items,
    prepare_targets,
)


class BackfillEdgeTtsTest(unittest.TestCase):
    def test_prepare_targets_normalizes_accent_and_skips_blank(self) -> None:
        raw_rows = [
            ("11111111-1111-1111-1111-111111111111", "story", "en-GB"),
            ("22222222-2222-2222-2222-222222222222", "story", "US"),
            ("33333333-3333-3333-3333-333333333333", "   ", "US"),
        ]
        targets, skipped = prepare_targets(raw_rows)
        self.assertEqual(skipped, 1)
        self.assertEqual(len(targets), 2)
        self.assertEqual(targets[0].accent, "UK")
        self.assertEqual(targets[1].accent, "US")
        self.assertTrue(targets[0].cache_key.startswith("uk_"))
        self.assertTrue(targets[1].cache_key.startswith("us_"))

    def test_build_unique_synthesis_items_skips_cached_and_duplicates(self) -> None:
        t1 = PronunciationTarget("id1", "story", "story", "US", "en-US-JennyNeural", "us_abc")
        t2 = PronunciationTarget("id2", "story", "story", "US", "en-US-JennyNeural", "us_abc")
        t3 = PronunciationTarget("id3", "find", "find", "UK", "en-GB-SoniaNeural", "uk_def")
        items = build_unique_synthesis_items([t1, t2, t3], {"uk_def": "https://cdn/uk_def.mp3"})
        self.assertEqual(len(items), 1)
        self.assertEqual(items[0]["cacheKey"], "us_abc")
        self.assertEqual(items[0]["text"], "story")


if __name__ == "__main__":
    unittest.main()
