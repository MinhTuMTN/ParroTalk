import asyncio
import hashlib
from io import StringIO
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from app import tts
from app.config import (
    TTS_ALLOWED_VOICES,
    TTS_ALLOWED_VOICES_UK,
    TTS_ALLOWED_VOICES_US,
    TTS_MAX_TEXT_LENGTH,
    TTS_VOICE_UK,
    TTS_VOICE_US,
)


def _make_fake_communicate(chunks: list[dict], calls: list[tuple[str, str]]):
    """Build a fake edge_tts.Communicate class recording init args and yielding chunks."""

    class FakeCommunicate:
        def __init__(self, text: str, voice: str):
            calls.append((text, voice))

        async def stream(self):
            for chunk in chunks:
                yield chunk

    return FakeCommunicate


class VoiceAndAccentTests(unittest.TestCase):
    def test_resolve_voice_maps_uk_variants(self):
        for variant in ("UK", "uk", "  gb ", "en-GB", "EN-GB", "en_gb", "EN_GB"):
            with self.subTest(variant=variant):
                self.assertEqual(tts.resolve_voice(variant), TTS_VOICE_UK)
                self.assertEqual(tts.normalize_accent(variant), "UK")

    def test_resolve_voice_defaults_to_us(self):
        for variant in ("US", "us", "en-US", None, "", "   ", "AU", "invalid"):
            with self.subTest(variant=variant):
                self.assertEqual(tts.resolve_voice(variant), TTS_VOICE_US)
                self.assertEqual(tts.normalize_accent(variant), "US")

    def test_resolve_voice_selects_allowlisted_voices_case_insensitively(self):
        self.assertEqual(TTS_ALLOWED_VOICES, TTS_ALLOWED_VOICES_US + TTS_ALLOWED_VOICES_UK)
        cases = [
            ("US", "en-US-GuyNeural", "en-US-GuyNeural"),
            ("US", "  EN-US-ARIANEURAL ", "en-US-AriaNeural"),
            ("UK", "en-GB-RyanNeural", "en-GB-RyanNeural"),
            ("UK", "en-gb-libbyneural", "en-GB-LibbyNeural"),
            ("en-GB", "en-GB-SoniaNeural", "en-GB-SoniaNeural"),
        ]
        for accent, raw_voice, expected in cases:
            with self.subTest(accent=accent, raw_voice=raw_voice):
                self.assertEqual(tts.resolve_voice(accent, raw_voice), expected)

    def test_resolve_voice_falls_back_on_accent_mismatch(self):
        cases = [
            ("US", "en-GB-RyanNeural", TTS_VOICE_US),
            ("US", "en-GB-SoniaNeural", TTS_VOICE_US),
            ("UK", "en-US-GuyNeural", TTS_VOICE_UK),
            ("en-GB", "en-US-AriaNeural", TTS_VOICE_UK),
        ]
        for accent, raw_voice, expected in cases:
            with self.subTest(accent=accent, raw_voice=raw_voice):
                self.assertEqual(tts.resolve_voice(accent, raw_voice), expected)

    def test_resolve_voice_falls_back_on_invalid_or_empty_voice(self):
        for invalid_voice in (None, "", "   ", "en-US-UnknownNeural"):
            with self.subTest(invalid_voice=invalid_voice):
                self.assertEqual(tts.resolve_voice("US", invalid_voice), TTS_VOICE_US)
                self.assertEqual(tts.resolve_voice("UK", invalid_voice), TTS_VOICE_UK)


class TextValidationTests(unittest.TestCase):
    def test_normalizes_whitespace_and_strips_control_chars(self):
        raw = "  Hello\x00\x07,\n\tworld\x0b\x0c   again\x1f!\x7f  "
        self.assertEqual(tts.validate_and_clean_text(raw), "Hello, world again!")

    def test_allows_exact_max_length(self):
        exact_text = "a" * TTS_MAX_TEXT_LENGTH
        self.assertEqual(tts.validate_and_clean_text(exact_text), exact_text)

    def test_rejects_text_exceeding_max_length(self):
        too_long = "a" * (TTS_MAX_TEXT_LENGTH + 1)
        with self.assertRaises(ValueError):
            tts.validate_and_clean_text(too_long)

    def test_rejects_empty_none_and_non_string_inputs(self):
        for bad_input in ("", "   \n\t ", "\x00\x01\x7f", None, 123, b"hello"):
            with self.subTest(bad_input=bad_input):
                with self.assertRaises(ValueError):
                    tts.validate_and_clean_text(bad_input)


class CacheKeyTests(unittest.TestCase):
    def test_cache_key_is_deterministic_across_equivalent_inputs(self):
        key_a = tts.compute_tts_cache_key("  Hello\tworld!  ", "en-GB")
        key_b = tts.compute_tts_cache_key("Hello world!", "uk")
        self.assertEqual(key_a, key_b)
        self.assertTrue(key_a.startswith("uk_"))
        self.assertEqual(len(key_a), 3 + 64)

    def test_cache_key_differentiates_accents(self):
        us_key = tts.compute_tts_cache_key("Practice daily.", "US")
        uk_key = tts.compute_tts_cache_key("Practice daily.", "UK")
        self.assertNotEqual(us_key, uk_key)
        self.assertTrue(us_key.startswith("us_"))
        self.assertTrue(uk_key.startswith("uk_"))

    def test_cache_key_preserves_text_case_for_acronyms(self):
        upper_us = tts.compute_tts_cache_key("US", "US")
        lower_us = tts.compute_tts_cache_key("us", "US")
        upper_it = tts.compute_tts_cache_key("IT", "UK")
        lower_it = tts.compute_tts_cache_key("it", "UK")

        self.assertNotEqual(upper_us, lower_us)
        self.assertNotEqual(upper_it, lower_it)
        expected_digest = hashlib.sha256(b"US:US").hexdigest()
        self.assertEqual(upper_us, f"us_{expected_digest}")

    def test_cache_key_backward_compatible_for_default_voice_and_distinct_for_custom(self):
        us_default_none = tts.compute_tts_cache_key("Hello world.", "US", None)
        us_default_explicit = tts.compute_tts_cache_key("Hello world.", "US", "en-US-JennyNeural")
        us_custom_guy = tts.compute_tts_cache_key("Hello world.", "US", "en-US-GuyNeural")

        self.assertEqual(us_default_none, us_default_explicit)
        self.assertNotEqual(us_default_none, us_custom_guy)
        expected_custom_digest = hashlib.sha256(
            b"US:en-US-GuyNeural:Hello world."
        ).hexdigest()
        self.assertEqual(us_custom_guy, f"us_{expected_custom_digest}")

        uk_default_none = tts.compute_tts_cache_key("Hello world.", "UK", None)
        uk_default_explicit = tts.compute_tts_cache_key("Hello world.", "UK", "en-GB-SoniaNeural")
        uk_custom_ryan = tts.compute_tts_cache_key("Hello world.", "UK", "en-gb-ryanneural")

        self.assertEqual(uk_default_none, uk_default_explicit)
        self.assertNotEqual(uk_default_none, uk_custom_ryan)
        self.assertTrue(uk_custom_ryan.startswith("uk_"))

        self.assertEqual(
            tts.compute_tts_cache_key("Hello world.", "US", "en-GB-RyanNeural"),
            us_default_none,
        )
        self.assertEqual(
            tts.compute_tts_cache_key("Hello world.", "UK", "en-US-GuyNeural"),
            uk_default_none,
        )


class SpeechSynthesisBytesTests(unittest.TestCase):
    def test_synthesize_speech_bytes_concatenates_audio_chunks(self):
        calls: list[tuple[str, str]] = []
        chunks = [
            {"type": "WordBoundary", "text": "Hello"},
            {"type": "audio", "data": b"part1-"},
            {"type": "audio", "data": "not-bytes"},
            {"type": "audio", "data": b""},
            {"type": "audio", "data": b"part2"},
        ]
        fake_cls = _make_fake_communicate(chunks, calls)

        with patch("app.tts.edge_tts.Communicate", fake_cls):
            result = tts.synthesize_speech_bytes("  Hello\nworld  ", accent="en-GB")

        self.assertEqual(result, b"part1-part2")
        self.assertEqual(calls, [("Hello world", TTS_VOICE_UK)])

    def test_synthesize_speech_bytes_forwards_custom_voice(self):
        calls: list[tuple[str, str]] = []
        fake_cls = _make_fake_communicate([{"type": "audio", "data": b"audio"}], calls)

        with patch("app.tts.edge_tts.Communicate", fake_cls):
            result = tts.synthesize_speech_bytes(
                "Hello world",
                accent="US",
                voice="en-us-arianeural",
            )

        self.assertEqual(result, b"audio")
        self.assertEqual(calls, [("Hello world", "en-US-AriaNeural")])

    def test_synthesize_speech_bytes_async_raises_on_empty_stream(self):
        calls: list[tuple[str, str]] = []
        chunks = [{"type": "WordBoundary", "text": "Hello"}]
        fake_cls = _make_fake_communicate(chunks, calls)

        with patch("app.tts.edge_tts.Communicate", fake_cls):
            with self.assertRaises(RuntimeError):
                asyncio.run(tts.synthesize_speech_bytes_async("Hello", accent="US"))

        self.assertEqual(calls, [("Hello", TTS_VOICE_US)])


class SpeechSynthesisFileTests(unittest.TestCase):
    def test_cache_hit_skips_synthesis(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            target = Path(tmp_dir) / "cached.mp3"
            target.write_bytes(b"cached-audio")

            with patch("app.tts.edge_tts.Communicate") as mock_communicate:
                result = tts.synthesize_speech_file("Hello", target, accent="US")

            self.assertEqual(result, target)
            self.assertEqual(target.read_bytes(), b"cached-audio")
            mock_communicate.assert_not_called()

    def test_cache_miss_creates_parent_dirs_and_writes_file(self):
        calls: list[tuple[str, str]] = []
        fake_cls = _make_fake_communicate(
            [{"type": "audio", "data": b"fresh-mp3-bytes"}],
            calls,
        )

        with tempfile.TemporaryDirectory() as tmp_dir:
            target = Path(tmp_dir) / "nested" / "dir" / "speech.mp3"

            with patch("app.tts.edge_tts.Communicate", fake_cls):
                result = tts.synthesize_speech_file("Hello world", target, accent="UK")

            self.assertEqual(result, target)
            self.assertEqual(target.read_bytes(), b"fresh-mp3-bytes")
            self.assertEqual(calls, [("Hello world", TTS_VOICE_UK)])
            self.assertEqual(list(target.parent.glob("*.tmp")), [])

    def test_synthesize_speech_file_forwards_custom_voice(self):
        calls: list[tuple[str, str]] = []
        fake_cls = _make_fake_communicate(
            [{"type": "audio", "data": b"custom-voice-bytes"}],
            calls,
        )

        with tempfile.TemporaryDirectory() as tmp_dir:
            target = Path(tmp_dir) / "custom.mp3"
            with patch("app.tts.edge_tts.Communicate", fake_cls):
                result = tts.synthesize_speech_file(
                    "Hello custom voice",
                    target,
                    accent="UK",
                    voice="en-GB-LibbyNeural",
                )

            self.assertEqual(result, target)
            self.assertEqual(target.read_bytes(), b"custom-voice-bytes")
            self.assertEqual(calls, [("Hello custom voice", "en-GB-LibbyNeural")])

    def test_replaces_existing_zero_byte_file(self):
        calls: list[tuple[str, str]] = []
        fake_cls = _make_fake_communicate(
            [{"type": "audio", "data": b"replacement-bytes"}],
            calls,
        )

        with tempfile.TemporaryDirectory() as tmp_dir:
            target = Path(tmp_dir) / "empty.mp3"
            target.write_bytes(b"")

            with patch("app.tts.edge_tts.Communicate", fake_cls):
                result = tts.synthesize_speech_file("Hello again", target, accent="US")

            self.assertEqual(result, target)
            self.assertEqual(target.read_bytes(), b"replacement-bytes")
            self.assertEqual(calls, [("Hello again", TTS_VOICE_US)])

    def test_failure_cleans_up_temp_file(self):
        calls: list[tuple[str, str]] = []
        fake_cls = _make_fake_communicate(
            [{"type": "audio", "data": b"audio-bytes"}],
            calls,
        )

        with tempfile.TemporaryDirectory() as tmp_dir:
            target = Path(tmp_dir) / "speech.mp3"
            with patch("app.tts.edge_tts.Communicate", fake_cls):
                with patch("app.tts.os.replace", side_effect=OSError("replace failed")):
                    with self.assertRaises(OSError):
                        tts.synthesize_speech_file("Hello", target, accent="US")

            self.assertFalse(target.exists())
            self.assertEqual(list(Path(tmp_dir).glob("*.tmp")), [])


class CliEntrypointTests(unittest.TestCase):
    def _assert_cli_success(
        self,
        argv_template: list[str],
        expected_text: str,
        expected_accent: str,
        expected_voice: str | None,
    ):
        with tempfile.TemporaryDirectory() as tmp_dir:
            output_path = Path(tmp_dir) / "out.mp3"
            argv = [arg.format(output=output_path) for arg in argv_template]
            stdout = StringIO()
            with patch("app.tts.synthesize_speech_file", return_value=output_path) as mock_synth:
                with patch("sys.stdout", stdout):
                    exit_code = tts.main(argv)

            self.assertEqual(exit_code, 0)
            self.assertEqual(stdout.getvalue().strip(), str(output_path))
            mock_synth.assert_called_once_with(
                text=expected_text,
                output_path=str(output_path),
                accent=expected_accent,
                voice=expected_voice,
            )

    def test_cli_returns_zero_on_success(self):
        self._assert_cli_success(
            ["--text", "Hello CLI", "--accent", "UK", "--output", "{output}"],
            expected_text="Hello CLI",
            expected_accent="UK",
            expected_voice=None,
        )

    def test_cli_accepts_voice_flag_forms(self):
        cases = [
            (
                "separate_voice",
                ["--text", "Hello", "--accent", "US", "--voice", "en-US-GuyNeural", "--output", "{output}"],
                "US",
                "en-US-GuyNeural",
            ),
            (
                "equals_voice",
                ["--text", "Hello", "--accent", "UK", "--voice=en-GB-RyanNeural", "--output", "{output}"],
                "UK",
                "en-GB-RyanNeural",
            ),
        ]
        for name, argv, expected_accent, expected_voice in cases:
            with self.subTest(case=name):
                self._assert_cli_success(
                    argv,
                    expected_text="Hello",
                    expected_accent=expected_accent,
                    expected_voice=expected_voice,
                )

    def test_cli_accepts_leading_hyphen_text_forms(self):
        cases = [
            ("separate_arg", ["--text", "-hello", "--output", "{output}"]),
            ("equals_arg", ["--text=-hello", "--output", "{output}"]),
        ]
        for name, argv in cases:
            with self.subTest(case=name):
                self._assert_cli_success(
                    argv,
                    expected_text="-hello",
                    expected_accent="US",
                    expected_voice=None,
                )

    def test_cli_preserves_help_and_rejects_unknown_flags(self):
        for flag in ("-h", "--help"):
            with self.subTest(flag=flag):
                stdout = StringIO()
                with patch("sys.stdout", stdout):
                    with self.assertRaises(SystemExit) as ctx:
                        tts.main([flag])
                self.assertEqual(ctx.exception.code, 0)
                self.assertIn("--text", stdout.getvalue())

        stderr = StringIO()
        with patch("sys.stderr", stderr):
            with self.assertRaises(SystemExit) as ctx:
                tts.main(["--text", "-hello", "--unknown", "x", "--output", "out.mp3"])
        self.assertNotEqual(ctx.exception.code, 0)
        self.assertIn("unrecognized arguments: --unknown x", stderr.getvalue())

    def test_cli_returns_nonzero_on_error(self):
        stderr = StringIO()
        with patch("app.tts.synthesize_speech_file", side_effect=ValueError("invalid text")):
            with patch("sys.stderr", stderr):
                exit_code = tts.main(["--text", " ", "--output", "out.mp3"])

        self.assertNotEqual(exit_code, 0)
        self.assertIn("invalid text", stderr.getvalue())


class CloudinaryAndWorkerTests(unittest.TestCase):
    def test_upload_tts_audio_to_cloudinary_uses_deterministic_public_id(self):
        with (
            patch("app.tts.CLOUDINARY_CLOUD_NAME", "demo"),
            patch("app.tts.CLOUDINARY_API_KEY", "key"),
            patch("app.tts.CLOUDINARY_API_SECRET", "secret"),
            patch("app.tts.CLOUDINARY_TTS_FOLDER", "tts"),
            patch("app.tts.cloudinary.config") as mock_config,
            patch(
                "app.tts.cloudinary.uploader.upload",
                return_value={"secure_url": "https://res.cloudinary.com/demo/video/upload/tts/us_abc.mp3"},
            ) as mock_upload,
        ):
            url = tts.upload_tts_audio_to_cloudinary(b"mp3-bytes", "us_abc")

        self.assertEqual(url, "https://res.cloudinary.com/demo/video/upload/tts/us_abc.mp3")
        mock_config.assert_called_once()
        kwargs = mock_upload.call_args.kwargs
        self.assertEqual(kwargs["public_id"], "tts/us_abc")
        self.assertEqual(kwargs["resource_type"], "video")
        self.assertEqual(kwargs["format"], "mp3")
        self.assertFalse(kwargs["overwrite"])
        self.assertFalse(kwargs["unique_filename"])

    def test_synthesize_and_upload_batch_runs_concurrently_and_skips_invalid(self):
        calls: list[tuple[str, str]] = []
        fake_cls = _make_fake_communicate(
            [{"type": "audio", "data": b"mp3-audio"}],
            calls,
        )

        with (
            patch("app.tts.edge_tts.Communicate", fake_cls),
            patch(
                "app.tts.upload_tts_audio_to_cloudinary",
                side_effect=lambda data, key: f"https://res.cloudinary.com/demo/video/upload/tts/{key}.mp3",
            ),
        ):
            results = tts.synthesize_and_upload_batch(
                [
                    {"text": "Hello", "accent": "US"},
                    {"text": "   ", "accent": "US"},
                    {"text": "Colour", "accent": "UK", "voice": "en-GB-RyanNeural"},
                ],
                concurrency=2,
            )

        self.assertEqual(len(results), 2)
        self.assertEqual(results[0]["accent"], "US")
        self.assertEqual(results[1]["accent"], "UK")
        self.assertEqual(results[1]["voice"], "en-GB-RyanNeural")
        self.assertTrue(results[0]["cloudinaryUrl"].endswith(f"/{results[0]['cacheKey']}.mp3"))

    def test_tts_worker_callback_publishes_results_and_acks(self):
        import json
        from types import SimpleNamespace
        from unittest.mock import MagicMock
        from app import tts_worker

        ch = MagicMock()
        method = SimpleNamespace(delivery_tag=42)
        body = json.dumps({"items": [{"cacheKey": "us_123", "text": "Hello", "accent": "US"}]}).encode("utf-8")

        with patch(
            "app.tts_worker.synthesize_and_upload_batch",
            return_value=[{"cacheKey": "us_123", "cloudinaryUrl": "https://cdn/us_123.mp3"}],
        ):
            tts_worker.callback(ch, method, None, body)

        ch.basic_publish.assert_called_once()
        published_body = json.loads(ch.basic_publish.call_args.kwargs["body"])
        self.assertEqual(published_body["results"][0]["cloudinaryUrl"], "https://cdn/us_123.mp3")
        ch.basic_ack.assert_called_once_with(delivery_tag=42)


if __name__ == "__main__":
    unittest.main()
