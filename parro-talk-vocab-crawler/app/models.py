from __future__ import annotations

from dataclasses import dataclass, field, asdict
from typing import Any


@dataclass
class CrawledPronunciation:
    accent: str
    ipa: str = ""
    audio_url: str = ""

    def to_dict(self) -> dict[str, Any]:
        return {
            "accent": self.accent,
            "ipa": self.ipa,
            "audioUrl": self.audio_url,
        }


@dataclass
class CrawledSense:
    part_of_speech: str = ""
    cefr_level: str = ""
    definition_en: str = ""
    example_en: str = ""
    synonyms: list[str] = field(default_factory=list)
    antonyms: list[str] = field(default_factory=list)
    collocations: list[str] = field(default_factory=list)
    topic: str = ""
    source: str = "OXFORD_CRAWLER"

    def to_dict(self) -> dict[str, Any]:
        return {
            "partOfSpeech": self.part_of_speech,
            "cefrLevel": self.cefr_level,
            "definitionEn": self.definition_en,
            "exampleEn": self.example_en,
            "synonyms": self.synonyms,
            "antonyms": self.antonyms,
            "collocations": self.collocations,
            "topic": self.topic,
            "source": self.source,
        }


@dataclass
class CrawledIdiomOrPhrasalVerb:
    phrase: str
    definition_en: str = ""
    example_en: str = ""
    source: str = "OXFORD_CRAWLER"

    def to_dict(self) -> dict[str, Any]:
        return {
            "phrase": self.phrase,
            "definitionEn": self.definition_en,
            "exampleEn": self.example_en,
            "source": self.source,
        }


@dataclass
class MwSenseSynAntGroup:
    part_of_speech: str = ""
    sense_definition: str = ""
    example_en: str = ""
    synonyms: list[str] = field(default_factory=list)
    antonyms: list[str] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return {
            "partOfSpeech": self.part_of_speech,
            "senseDefinition": self.sense_definition,
            "exampleEn": self.example_en,
            "synonyms": self.synonyms,
            "antonyms": self.antonyms,
        }


@dataclass
class OxfordEntryData:
    word: str
    pronunciations: list[CrawledPronunciation] = field(default_factory=list)
    senses: list[CrawledSense] = field(default_factory=list)
    idioms: list[CrawledIdiomOrPhrasalVerb] = field(default_factory=list)
    phrasal_verbs: list[CrawledIdiomOrPhrasalVerb] = field(default_factory=list)


@dataclass
class MwDictionaryData:
    word: str
    word_forms: list[str] = field(default_factory=list)
    pronunciations: list[CrawledPronunciation] = field(default_factory=list)
    senses: list[CrawledSense] = field(default_factory=list)
    idioms: list[CrawledIdiomOrPhrasalVerb] = field(default_factory=list)
    phrasal_verbs: list[CrawledIdiomOrPhrasalVerb] = field(default_factory=list)


@dataclass
class MergedCrawlPayload:
    word: str
    pronunciations: list[CrawledPronunciation] = field(default_factory=list)
    word_forms: list[str] = field(default_factory=list)
    senses: list[CrawledSense] = field(default_factory=list)
    mw_sense_syn_ant_groups: list[MwSenseSynAntGroup] = field(default_factory=list)
    idioms: list[CrawledIdiomOrPhrasalVerb] = field(default_factory=list)
    phrasal_verbs: list[CrawledIdiomOrPhrasalVerb] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return {
            "word": self.word,
            "pronunciations": [p.to_dict() for p in self.pronunciations],
            "wordForms": self.word_forms,
            "senses": [s.to_dict() for s in self.senses],
            "mwSenseSynAntGroups": [g.to_dict() for g in self.mw_sense_syn_ant_groups],
            "idioms": [i.to_dict() for i in self.idioms],
            "phrasalVerbs": [pv.to_dict() for pv in self.phrasal_verbs],
        }


@dataclass
class CrawlJobResult:
    word: str
    status: str  # "SUCCESS" | "NOT_FOUND" | "FAILED"
    data_source: str  # "OXFORD_MW_HYBRID" | "OXFORD_CRAWLER" | "MERRIAM_WEBSTER_API" | "NOT_FOUND"
    payload: MergedCrawlPayload
    error: str | None = None

    def to_message_dict(
        self,
        job_id: str | None = None,
        list_id: str | None = None,
        list_item_id: str | None = None,
        user_id: str | None = None,
    ) -> dict[str, Any]:
        return {
            "jobId": job_id,
            "listId": list_id,
            "listItemId": list_item_id,
            "userId": user_id,
            "word": self.word,
            "status": self.status,
            "dataSource": self.data_source,
            "error": self.error,
            "payload": self.payload.to_dict(),
        }

