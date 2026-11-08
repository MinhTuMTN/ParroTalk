from __future__ import annotations

import logging
import re
import urllib.parse
from typing import Any
import httpx

from app.config import HTTP_TIMEOUT_SECONDS, MW_DICTIONARY_API_KEY, MW_THESAURUS_API_KEY
from app.models import (
    CrawledIdiomOrPhrasalVerb,
    CrawledPronunciation,
    CrawledSense,
    MwDictionaryData,
    MwSenseSynAntGroup,
)

logger = logging.getLogger(__name__)

MW_THESAURUS_URL = "https://www.dictionaryapi.com/api/v3/references/thesaurus/json"
MW_COLLEGIATE_URL = "https://www.dictionaryapi.com/api/v3/references/collegiate/json"
MW_AUDIO_BASE = "https://media.merriam-webster.com/audio/prons/en/us/mp3"

_MW_TAG_RE = re.compile(r"\{[^}]+\}")


def clean_mw_markup(text: str) -> str:
    if not text:
        return ""
    text = text.replace("{bc}", ": ")
    cleaned = _MW_TAG_RE.sub("", text)
    cleaned = re.sub(r"^:\s*", "", cleaned.strip())
    return " ".join(cleaned.split())


def _build_mw_audio_url(audio_code: str) -> str:
    if not audio_code:
        return ""
    if audio_code.startswith("bix"):
        sub = "bix"
    elif audio_code.startswith("gg"):
        sub = "gg"
    elif not audio_code[0].isalpha():
        sub = "number"
    else:
        sub = audio_code[0]
    return f"{MW_AUDIO_BASE}/{sub}/{audio_code}.mp3"


def _extract_dt_def_and_example(dt_list: list[Any]) -> tuple[str, str]:
    definition = ""
    example = ""
    for item in dt_list or []:
        if not isinstance(item, list) or len(item) < 2:
            continue
        tag, content = item[0], item[1]
        if tag == "text" and isinstance(content, str) and not definition:
            definition = clean_mw_markup(content)
        elif tag == "vis" and isinstance(content, list) and not example:
            for v in content:
                if isinstance(v, dict) and v.get("t"):
                    example = clean_mw_markup(str(v["t"]))
                    break
    return definition, example


def parse_mw_thesaurus_json(data: list[Any], target_word: str) -> list[MwSenseSynAntGroup]:
    if not isinstance(data, list):
        return []

    groups: list[MwSenseSynAntGroup] = []
    for entry in data:
        if not isinstance(entry, dict):
            continue
        pos = str(entry.get("fl", "")).strip().lower()
        for def_block in entry.get("def", []) or []:
            if not isinstance(def_block, dict):
                continue
            for sseq_group in def_block.get("sseq", []) or []:
                for item in sseq_group or []:
                    if not isinstance(item, list) or len(item) < 2 or item[0] != "sense":
                        continue
                    sense_obj = item[1]
                    if not isinstance(sense_obj, dict):
                        continue
                    sense_def, sense_ex = _extract_dt_def_and_example(sense_obj.get("dt", []))

                    syns: list[str] = []
                    for syn_sublist in sense_obj.get("syn_list", []) or []:
                        for w_obj in syn_sublist or []:
                            if isinstance(w_obj, dict) and w_obj.get("wd"):
                                w = str(w_obj["wd"]).strip()
                                if w and w not in syns:
                                    syns.append(w)
                    for sim_sublist in sense_obj.get("sim_list", []) or []:
                        for w_obj in sim_sublist or []:
                            if isinstance(w_obj, dict) and w_obj.get("wd"):
                                w = str(w_obj["wd"]).strip()
                                if w and w not in syns:
                                    syns.append(w)

                    ants: list[str] = []
                    for opp_sublist in sense_obj.get("opp_list", []) or []:
                        for w_obj in opp_sublist or []:
                            if isinstance(w_obj, dict) and w_obj.get("wd"):
                                w = str(w_obj["wd"]).strip()
                                if w and w not in ants:
                                    ants.append(w)
                    for ant_sublist in sense_obj.get("ant_list", []) or []:
                        for w_obj in ant_sublist or []:
                            if isinstance(w_obj, dict) and w_obj.get("wd"):
                                w = str(w_obj["wd"]).strip()
                                if w and w not in ants:
                                    ants.append(w)

                    if sense_def or syns or ants:
                        groups.append(
                            MwSenseSynAntGroup(
                                part_of_speech=pos,
                                sense_definition=sense_def,
                                example_en=sense_ex,
                                synonyms=syns[:12],
                                antonyms=ants[:12],
                            )
                        )
    return groups


def parse_mw_dictionary_json(data: list[Any], target_word: str) -> MwDictionaryData | None:
    if not isinstance(data, list) or not data:
        return None

    word_forms: list[str] = []
    pronunciations: list[CrawledPronunciation] = []
    senses: list[CrawledSense] = []
    idioms: list[CrawledIdiomOrPhrasalVerb] = []
    phrasal_verbs: list[CrawledIdiomOrPhrasalVerb] = []

    found_valid_entry = False
    for entry in data:
        if not isinstance(entry, dict):
            continue
        found_valid_entry = True
        pos = str(entry.get("fl", "")).strip().lower()

        meta = entry.get("meta") or {}
        for stem in meta.get("stems", []) or []:
            clean_stem = str(stem).replace("*", "").strip()
            if clean_stem and clean_stem not in word_forms:
                word_forms.append(clean_stem)

        for uro in entry.get("uros", []) or []:
            if isinstance(uro, dict) and uro.get("ure"):
                ure_word = str(uro["ure"]).replace("*", "").strip()
                if ure_word and ure_word not in word_forms:
                    word_forms.append(ure_word)

        hwi = entry.get("hwi") or {}
        for pr in hwi.get("prs", []) or []:
            if isinstance(pr, dict):
                ipa = str(pr.get("mw", "")).strip()
                sound_obj = pr.get("sound") or {}
                audio_code = str(sound_obj.get("audio", "")).strip() if isinstance(sound_obj, dict) else ""
                audio_url = _build_mw_audio_url(audio_code)
                if (ipa or audio_url) and not pronunciations:
                    pronunciations.append(
                        CrawledPronunciation(
                            accent="US",
                            ipa=f"/{ipa}/" if ipa and not ipa.startswith("/") else ipa,
                            audio_url=audio_url,
                        )
                    )

        for def_block in entry.get("def", []) or []:
            if not isinstance(def_block, dict):
                continue
            for sseq_group in def_block.get("sseq", []) or []:
                for item in sseq_group or []:
                    if not isinstance(item, list) or len(item) < 2 or item[0] != "sense":
                        continue
                    sense_obj = item[1]
                    if not isinstance(sense_obj, dict):
                        continue
                    sense_def, sense_ex = _extract_dt_def_and_example(sense_obj.get("dt", []))
                    if sense_def:
                        senses.append(
                            CrawledSense(
                                part_of_speech=pos or "noun",
                                cefr_level="",
                                definition_en=sense_def,
                                example_en=sense_ex,
                                source="MERRIAM_WEBSTER_API",
                            )
                        )

        for dro in entry.get("dros", []) or []:
            if not isinstance(dro, dict) or not dro.get("drp"):
                continue
            phrase = str(dro["drp"]).replace("*", "").strip()
            dro_def = ""
            dro_ex = ""
            for def_block in dro.get("def", []) or []:
                for sseq_group in (def_block.get("sseq", []) if isinstance(def_block, dict) else []):
                    for item in sseq_group or []:
                        if isinstance(item, list) and len(item) >= 2 and item[0] == "sense" and isinstance(item[1], dict):
                            dro_def, dro_ex = _extract_dt_def_and_example(item[1].get("dt", []))
                            if dro_def:
                                break
                    if dro_def:
                        break
            if dro_def:
                item_obj = CrawledIdiomOrPhrasalVerb(
                    phrase=phrase,
                    definition_en=dro_def,
                    example_en=dro_ex,
                    source="MERRIAM_WEBSTER_API",
                )
                if len(phrase.split()) == 2:
                    phrasal_verbs.append(item_obj)
                else:
                    idioms.append(item_obj)

    if not found_valid_entry:
        return None

    return MwDictionaryData(
        word=target_word.strip(),
        word_forms=word_forms,
        pronunciations=pronunciations,
        senses=senses,
        idioms=idioms,
        phrasal_verbs=phrasal_verbs,
    )


def fetch_merriam_webster_data(
    word: str,
    thesaurus_key: str | None = None,
    dictionary_key: str | None = None,
) -> tuple[list[MwSenseSynAntGroup], MwDictionaryData | None]:
    t_key = (thesaurus_key if thesaurus_key is not None else MW_THESAURUS_API_KEY).strip()
    d_key = (dictionary_key if dictionary_key is not None else MW_DICTIONARY_API_KEY).strip()
    encoded = urllib.parse.quote(word.strip().lower())

    thes_groups: list[MwSenseSynAntGroup] = []
    dict_data: MwDictionaryData | None = None

    with httpx.Client(timeout=HTTP_TIMEOUT_SECONDS) as client:
        if t_key:
            try:
                resp = client.get(f"{MW_THESAURUS_URL}/{encoded}", params={"key": t_key})
                if resp.status_code == 200:
                    text_content = resp.text.strip()
                    if text_content.startswith("[") or text_content.startswith("{"):
                        thes_groups = parse_mw_thesaurus_json(resp.json(), word)
                    else:
                        logger.warning("MW Thesaurus returned non-JSON response for '%s': %s", word, text_content[:150])
                else:
                    logger.warning("MW Thesaurus HTTP %s for '%s'", resp.status_code, word)
            except Exception as exc:
                logger.warning("MW Thesaurus fetch failed for '%s': %s", word, exc)

        if d_key:
            try:
                resp = client.get(f"{MW_COLLEGIATE_URL}/{encoded}", params={"key": d_key})
                if resp.status_code == 200:
                    text_content = resp.text.strip()
                    if text_content.startswith("[") or text_content.startswith("{"):
                        dict_data = parse_mw_dictionary_json(resp.json(), word)
                    else:
                        logger.warning("MW Dictionary returned non-JSON response for '%s': %s", word, text_content[:150])
                else:
                    logger.warning("MW Dictionary HTTP %s for '%s'", resp.status_code, word)
            except Exception as exc:
                logger.warning("MW Dictionary fetch failed for '%s': %s", word, exc)

    return thes_groups, dict_data

