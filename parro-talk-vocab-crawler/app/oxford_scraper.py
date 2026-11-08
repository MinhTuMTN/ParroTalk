from __future__ import annotations

import logging
import urllib.parse
from bs4 import BeautifulSoup, Tag
import httpx

from app.config import HTTP_TIMEOUT_SECONDS
from app.models import (
    CrawledIdiomOrPhrasalVerb,
    CrawledPronunciation,
    CrawledSense,
    OxfordEntryData,
)

logger = logging.getLogger(__name__)

OXFORD_BASE_URL = "https://www.oxfordlearnersdictionaries.com/definition/english"
DEFAULT_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/130.0.0.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.9",
}


def _clean_text(node: Tag | None) -> str:
    if node is None:
        return ""
    return " ".join(node.get_text(" ", strip=True).split())


def parse_oxford_html(html: str, fallback_word: str) -> OxfordEntryData | None:
    if not html or not html.strip():
        return None

    soup = BeautifulSoup(html, "html.parser")
    entry = soup.select_one("div.entry")
    if entry is None:
        return None

    headword_node = entry.select_one("h1.headword")
    word = _clean_text(headword_node) or fallback_word.strip()
    pos_node = entry.select_one("span.pos")
    default_pos = _clean_text(pos_node).lower()

    pronunciations: list[CrawledPronunciation] = []
    for accent_cls, accent_label in (("div.phons_br", "UK"), ("div.phons_n_am", "US")):
        phon_box = entry.select_one(accent_cls)
        if phon_box:
            ipa = _clean_text(phon_box.select_one("span.phon"))
            audio_btn = phon_box.select_one("[data-src-mp3]")
            audio_url = str(audio_btn.get("data-src-mp3", "")).strip() if audio_btn else ""
            if ipa or audio_url:
                pronunciations.append(
                    CrawledPronunciation(accent=accent_label, ipa=ipa, audio_url=audio_url)
                )

    senses: list[CrawledSense] = []
    sense_nodes = entry.select("li.sense")
    for s_node in sense_nodes:
        if s_node.find_parent(class_="idm-g") or s_node.find_parent(class_="pv-g"):
            continue

        def_node = s_node.select_one("span.def")
        definition_en = _clean_text(def_node)
        if not definition_en:
            continue

        cefr = str(s_node.get("cefr", "")).upper().strip()
        if not cefr:
            sym_node = s_node.select_one("span.symbols span[class*='ox']")
            if sym_node:
                cefr = _clean_text(sym_node).upper()

        example_node = s_node.select_one("ul.examples li span.x, ul.examples span.x, li span.x")
        example_en = _clean_text(example_node)

        synonyms: list[str] = []
        for syn_node in s_node.select("div.xrefs[xt='syn'] span.xh, div.xrefs[xt='syn'] a"):
            txt = _clean_text(syn_node)
            if txt and txt.lower() != "synonym" and txt not in synonyms:
                synonyms.append(txt)

        antonyms: list[str] = []
        for opp_node in s_node.select("div.xrefs[xt='opp'] span.xh, div.xrefs[xt='opp'] a"):
            txt = _clean_text(opp_node)
            if txt and txt.lower() != "opposite" and txt not in antonyms:
                antonyms.append(txt)

        collocations: list[str] = []
        for col_node in s_node.select("span.cf, ul.examples span.cf, div[unbox='extra_examples'] span.unx"):
            txt = _clean_text(col_node)
            if txt and txt not in collocations:
                collocations.append(txt)

        topic_node = s_node.select_one("span.topic-g a.topic, a.topic")
        topic = _clean_text(topic_node)

        senses.append(
            CrawledSense(
                part_of_speech=default_pos or "noun",
                cefr_level=cefr if cefr in {"A1", "A2", "B1", "B2", "C1", "C2"} else "",
                definition_en=definition_en,
                example_en=example_en,
                synonyms=synonyms,
                antonyms=antonyms,
                collocations=collocations[:8],
                topic=topic,
                source="OXFORD_CRAWLER",
            )
        )

    idioms: list[CrawledIdiomOrPhrasalVerb] = []
    for idm_group in entry.select("div.idm-g, span.idm-g"):
        phrase = _clean_text(idm_group.select_one("span.idm"))
        if not phrase:
            continue
        idm_def = _clean_text(idm_group.select_one("span.def"))
        idm_ex = _clean_text(idm_group.select_one("ul.examples li span.x, .x"))
        if idm_def:
            idioms.append(
                CrawledIdiomOrPhrasalVerb(
                    phrase=phrase,
                    definition_en=idm_def,
                    example_en=idm_ex,
                    source="OXFORD_CRAWLER",
                )
            )

    phrasal_verbs: list[CrawledIdiomOrPhrasalVerb] = []
    for pv_group in entry.select("div.pv-g, span.pv-g"):
        phrase = _clean_text(pv_group.select_one("span.pv, h4.pv"))
        if not phrase:
            continue
        pv_def = _clean_text(pv_group.select_one("span.def"))
        pv_ex = _clean_text(pv_group.select_one("ul.examples li span.x, .x"))
        if pv_def:
            phrasal_verbs.append(
                CrawledIdiomOrPhrasalVerb(
                    phrase=phrase,
                    definition_en=pv_def,
                    example_en=pv_ex,
                    source="OXFORD_CRAWLER",
                )
            )

    if not senses and not idioms and not phrasal_verbs:
        return None

    return OxfordEntryData(
        word=word,
        pronunciations=pronunciations,
        senses=senses,
        idioms=idioms,
        phrasal_verbs=phrasal_verbs,
    )


def fetch_oxford_word(word: str) -> OxfordEntryData | None:
    slug = urllib.parse.quote(word.strip().lower().replace(" ", "-"))
    merged: OxfordEntryData | None = None

    with httpx.Client(timeout=HTTP_TIMEOUT_SECONDS, headers=DEFAULT_HEADERS, follow_redirects=True) as client:
        for idx in range(1, 4):
            suffix = f"_{idx}" if idx > 1 else "_1"
            url = f"{OXFORD_BASE_URL}/{slug}{suffix}"
            try:
                resp = client.get(url)
                if resp.status_code == 404 and idx == 1:
                    resp = client.get(f"{OXFORD_BASE_URL}/{slug}")
                if resp.status_code != 200:
                    break
                parsed = parse_oxford_html(resp.text, word)
                if parsed is None:
                    break
                if merged is None:
                    merged = parsed
                else:
                    if not merged.pronunciations and parsed.pronunciations:
                        merged.pronunciations = parsed.pronunciations
                    existing_defs = {s.definition_en for s in merged.senses}
                    for s in parsed.senses:
                        if s.definition_en not in existing_defs:
                            merged.senses.append(s)
                            existing_defs.add(s.definition_en)
                    existing_idioms = {i.phrase for i in merged.idioms}
                    for idm in parsed.idioms:
                        if idm.phrase not in existing_idioms:
                            merged.idioms.append(idm)
                            existing_idioms.add(idm.phrase)
                    existing_pvs = {p.phrase for p in merged.phrasal_verbs}
                    for pv in parsed.phrasal_verbs:
                        if pv.phrase not in existing_pvs:
                            merged.phrasal_verbs.append(pv)
                            existing_pvs.add(pv.phrase)
            except Exception as exc:
                logger.warning("Oxford fetch error for '%s' (%s): %s", word, url, exc)
                break

    return merged

