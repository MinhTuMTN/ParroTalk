from __future__ import annotations

import logging
from typing import Any
import httpx

from app.config import DATAMUSE_API_URL, DATAMUSE_TIMEOUT_SECONDS

logger = logging.getLogger(__name__)

POS_MAP = {
    "noun": "n",
    "verb": "v",
    "adjective": "adj",
    "adj": "adj",
    "adverb": "adv",
    "adv": "adv",
}

TAG_TO_POS = {
    "n": "noun",
    "v": "verb",
    "adj": "adjective",
    "adv": "adverb",
}


def _matches_pos(tags: list[str], target_pos: str) -> bool:
    if not target_pos:
        return True
    expected_tag = POS_MAP.get(target_pos.lower(), target_pos.lower())
    return expected_tag in tags


class DatamuseClient:
    def __init__(self, base_url: str = DATAMUSE_API_URL, timeout: float = DATAMUSE_TIMEOUT_SECONDS) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout = min(timeout, 3.0)

    def fetch_synonyms(self, word: str, part_of_speech: str = "", limit: int = 10) -> list[str]:
        clean_word = word.strip().lower()
        if not clean_word:
            return []

        synonyms: list[str] = []
        try:
            with httpx.Client(timeout=self.timeout) as client:
                resp = client.get(self.base_url, params={"rel_syn": clean_word, "md": "p"})
                if resp.status_code == 200:
                    for item in resp.json():
                        w = item.get("word", "").strip()
                        tags = item.get("tags", [])
                        if w and w != clean_word and _matches_pos(tags, part_of_speech):
                            if w not in synonyms:
                                synonyms.append(w)
                        if len(synonyms) >= limit:
                            break

                # If needed, fallback to ml (means like)
                if len(synonyms) < 5:
                    resp_ml = client.get(self.base_url, params={"ml": clean_word, "md": "p"})
                    if resp_ml.status_code == 200:
                        for item in resp_ml.json():
                            w = item.get("word", "").strip()
                            tags = item.get("tags", [])
                            if w and w != clean_word and _matches_pos(tags, part_of_speech):
                                if w not in synonyms:
                                    synonyms.append(w)
                            if len(synonyms) >= limit:
                                break
        except Exception as exc:
            logger.warning("Datamuse fetch_synonyms failed for '%s': %s", clean_word, exc)

        return synonyms[:limit]

    def fetch_antonyms(self, word: str, part_of_speech: str = "", limit: int = 5) -> list[str]:
        clean_word = word.strip().lower()
        if not clean_word:
            return []

        antonyms: list[str] = []
        try:
            with httpx.Client(timeout=self.timeout) as client:
                resp = client.get(self.base_url, params={"rel_ant": clean_word, "md": "p"})
                if resp.status_code == 200:
                    for item in resp.json():
                        w = item.get("word", "").strip()
                        tags = item.get("tags", [])
                        if w and w != clean_word and _matches_pos(tags, part_of_speech):
                            if w not in antonyms:
                                antonyms.append(w)
                        if len(antonyms) >= limit:
                            break
        except Exception as exc:
            logger.warning("Datamuse fetch_antonyms failed for '%s': %s", clean_word, exc)

        return antonyms[:limit]

    def fetch_word_forms(self, word: str) -> list[dict[str, str]]:
        clean_word = word.strip().lower()
        if not clean_word or len(clean_word) < 3:
            return []

        # Use prefix matching: first 4-5 chars or length - 2
        prefix_len = max(3, len(clean_word) - 2)
        prefix = clean_word[:prefix_len]
        pattern = f"{prefix}*"

        word_forms: list[dict[str, str]] = []
        seen_words: set[str] = set()

        try:
            with httpx.Client(timeout=self.timeout) as client:
                resp = client.get(self.base_url, params={"sp": pattern, "md": "p"})
                if resp.status_code == 200:
                    for item in resp.json():
                        w = item.get("word", "").strip()
                        tags = item.get("tags", [])
                        if not w or w in seen_words:
                            continue

                        # Extract first recognized part of speech
                        pos = ""
                        for tag in tags:
                            if tag in TAG_TO_POS:
                                pos = TAG_TO_POS[tag]
                                break

                        if pos:
                            seen_words.add(w)
                            word_forms.append({"word": w, "partOfSpeech": pos})
                        if len(word_forms) >= 8:
                            break
        except Exception as exc:
            logger.warning("Datamuse fetch_word_forms failed for '%s': %s", clean_word, exc)

        return word_forms
