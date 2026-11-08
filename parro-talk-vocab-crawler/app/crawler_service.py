from __future__ import annotations

import logging
from app.datamuse_client import DatamuseClient
from app.merriam_webster_client import fetch_merriam_webster_data
from app.models import (
    CrawlJobResult,
    MergedCrawlPayload,
    MwDictionaryData,
    MwSenseSynAntGroup,
    OxfordEntryData,
    WordForm,
)
from app.oxford_scraper import fetch_oxford_word

logger = logging.getLogger(__name__)


def merge_crawl_results(
    word: str,
    oxford_data: OxfordEntryData | None,
    mw_thesaurus_groups: list[MwSenseSynAntGroup],
    mw_dict_data: MwDictionaryData | None,
) -> CrawlJobResult:
    clean_word = word.strip()
    has_oxford = oxford_data is not None and bool(
        oxford_data.senses or oxford_data.idioms or oxford_data.phrasal_verbs
    )
    has_mw = bool(mw_thesaurus_groups) or (
        mw_dict_data is not None and bool(mw_dict_data.senses or mw_dict_data.word_forms)
    )

    if not has_oxford and not has_mw:
        return CrawlJobResult(
            word=clean_word,
            status="NOT_FOUND",
            data_source="NOT_FOUND",
            payload=MergedCrawlPayload(word=clean_word),
        )

    if has_oxford and has_mw:
        data_source = "OXFORD_MW_HYBRID"
    elif has_oxford:
        data_source = "OXFORD_CRAWLER"
    else:
        data_source = "MERRIAM_WEBSTER_API"

    pronunciations = list(oxford_data.pronunciations) if oxford_data and oxford_data.pronunciations else (
        list(mw_dict_data.pronunciations) if mw_dict_data else []
    )
    word_forms = list(oxford_data.word_forms) if oxford_data and oxford_data.word_forms else (
        list(mw_dict_data.word_forms) if mw_dict_data else []
    )

    if has_oxford and oxford_data:
        senses = list(oxford_data.senses)
    elif mw_dict_data and mw_dict_data.senses:
        senses = list(mw_dict_data.senses)
    else:
        senses = []

    idioms = list(oxford_data.idioms) if oxford_data else []
    if mw_dict_data:
        existing_idiom_phrases = {i.phrase.lower() for i in idioms}
        for idm in mw_dict_data.idioms:
            if idm.phrase.lower() not in existing_idiom_phrases:
                idioms.append(idm)
                existing_idiom_phrases.add(idm.phrase.lower())

    phrasal_verbs = list(oxford_data.phrasal_verbs) if oxford_data else []
    if mw_dict_data:
        existing_pv_phrases = {p.phrase.lower() for p in phrasal_verbs}
        for pv in mw_dict_data.phrasal_verbs:
            if pv.phrase.lower() not in existing_pv_phrases:
                phrasal_verbs.append(pv)
                existing_pv_phrases.add(pv.phrase.lower())

    return CrawlJobResult(
        word=oxford_data.word if oxford_data else clean_word,
        status="SUCCESS",
        data_source=data_source,
        payload=MergedCrawlPayload(
            word=oxford_data.word if oxford_data else clean_word,
            pronunciations=pronunciations,
            word_forms=word_forms,
            senses=senses,
            mw_sense_syn_ant_groups=list(mw_thesaurus_groups or []),
            idioms=idioms,
            phrasal_verbs=phrasal_verbs,
        ),
    )


class CrawlService:
    def __init__(
        self,
        datamuse_client: DatamuseClient | None = None,
    ) -> None:
        self.datamuse_client = datamuse_client or DatamuseClient()

    def enrich_oxford_data(self, oxford_data: OxfordEntryData) -> OxfordEntryData:
        try:
            datamuse_wf = self.datamuse_client.fetch_word_forms(oxford_data.word)
            if datamuse_wf and not oxford_data.word_forms:
                oxford_data.word_forms = [
                    WordForm(word=item["word"], part_of_speech=item.get("partOfSpeech", ""))
                    for item in datamuse_wf
                    if item.get("word")
                ]
        except Exception as exc:
            logger.warning("Datamuse word forms enrichment failed for '%s': %s", oxford_data.word, exc)
        return oxford_data

    def crawl_word(self, word: str) -> CrawlJobResult:
        oxford_data = fetch_oxford_word(word)
        if oxford_data:
            oxford_data = self.enrich_oxford_data(oxford_data)
        mw_thesaurus_groups, mw_dict_data = fetch_merriam_webster_data(word)
        return merge_crawl_results(word, oxford_data, mw_thesaurus_groups, mw_dict_data)
