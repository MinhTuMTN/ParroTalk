import pytest
from fastapi.testclient import TestClient

from app.oxford_scraper import parse_oxford_html
from app.merriam_webster_client import parse_mw_thesaurus_json, parse_mw_dictionary_json
from app.crawler_service import merge_crawl_results, CrawlService
from main import app


SAMPLE_OXFORD_HTML = """
<!DOCTYPE html>
<html>
<body>
  <div class="entry">
    <div class="top-container">
      <div class="webtop">
        <h1 class="headword">resilient</h1>
        <span class="pos">adjective</span>
        <span class="phonetics">
          <div class="phons_br">
            <div class="sound audio_play_button pron-uk" data-src-mp3="https://www.oxfordlearnersdictionaries.com/media/english/uk_pron/r/res/resil/resilient__gb_1.mp3"></div>
            <span class="phon">/rɪˈzɪliənt/</span>
          </div>
          <div class="phons_n_am">
            <div class="sound audio_play_button pron-us" data-src-mp3="https://www.oxfordlearnersdictionaries.com/media/english/us_pron/r/res/resil/resilient__us_1.mp3"></div>
            <span class="phon">/rɪˈzɪliənt/</span>
          </div>
        </span>
      </div>
    </div>
    <ol class="senses_multiple">
      <li class="sense" cefr="b2">
        <span class="sensetop"><span class="symbols"><a href="#"><span class="ox3000">b2</span></a></span></span>
        <span class="def">able to feel better quickly after something unpleasant such as shock, injury, etc.</span>
        <ul class="examples">
          <li><span class="x">He'll get over it—young people are amazingly resilient.</span></li>
        </ul>
        <div class="xrefs" xt="syn">
          <span class="prefix">synonym</span>
          <a href="#"><span class="xh">tough</span></a>
        </div>
        <div class="xrefs" xt="opp">
          <span class="prefix">opposite</span>
          <a href="#"><span class="xh">fragile</span></a>
        </div>
        <span class="topic-g"><a class="topic" href="#">Personal qualities</a></span>
      </li>
      <li class="sense" cefr="c1">
        <span class="def">able to return to its original shape after being bent, stretched or pressed</span>
        <ul class="examples">
          <li><span class="x">resilient materials</span></li>
        </ul>
      </li>
    </ol>
    <div class="idioms">
      <div class="idm-g">
        <div class="top-container"><span class="idm">bounce back resiliently</span></div>
        <ol class="sense_single">
          <li class="sense">
            <span class="def">to recover quickly from a setback</span>
            <ul class="examples"><li><span class="x">The team bounced back resiliently after the defeat.</span></li></ul>
          </li>
        </ol>
      </div>
    </div>
  </div>
</body>
</html>
"""

SAMPLE_MW_THESAURUS_JSON = [
    {
        "meta": {
            "id": "resilient",
            "stems": ["resilient", "resiliently", "resilience"],
            "syns": [["adaptable", "buoyant", "tough"], ["elastic", "flexible", "springy"]],
            "ants": [["delicate", "fragile", "vulnerable"], ["rigid", "stiff"]],
        },
        "fl": "adjective",
        "def": [
            {
                "sseq": [
                    [
                        [
                            "sense",
                            {
                                "dt": [
                                    ["text", "able to recover rapidly from illness, change, or misfortune"],
                                    ["vis", [{"t": "She is a {it}resilient{/it} athlete."}]]
                                ],
                                "syn_list": [[{"wd": "adaptable"}, {"wd": "buoyant"}, {"wd": "tough"}]],
                                "opp_list": [[{"wd": "delicate"}, {"wd": "fragile"}, {"wd": "vulnerable"}]]
                            }
                        ]
                    ],
                    [
                        [
                            "sense",
                            {
                                "dt": [
                                    ["text", "able to resume an original shape after compression"]
                                ],
                                "syn_list": [[{"wd": "elastic"}, {"wd": "flexible"}, {"wd": "springy"}]],
                                "opp_list": [[{"wd": "rigid"}, {"wd": "stiff"}]]
                            }
                        ]
                    ]
                ]
            }
        ]
    }
]

SAMPLE_MW_DICTIONARY_JSON = [
    {
        "meta": {
            "id": "resilient:1",
            "stems": ["resilient", "resilience", "resiliency", "resiliently"]
        },
        "fl": "adjective",
        "hwi": {
            "hw": "re*sil*ient",
            "prs": [
                {
                    "mw": "ri-ˈzil-yənt",
                    "sound": {"audio": "resili02"}
                }
            ]
        },
        "def": [
            {
                "sseq": [
                    [
                        [
                            "sense",
                            {
                                "dt": [
                                    ["text", "{bc}tending to recover from or adjust easily to misfortune or change"],
                                    ["vis", [{"t": "children are often very {wi}resilient{/wi}"}]]
                                ]
                            }
                        ]
                    ]
                ]
            }
        ],
        "uros": [
            {"ure": "re*sil*ient*ly", "fl": "adverb"},
            {"ure": "re*sil*ience", "fl": "noun"}
        ],
        "dros": [
            {
                "drp": "resilient up",
                "vrs": [{"va": "resilient up"}],
                "def": [
                    {
                        "sseq": [
                            [
                                [
                                    "sense",
                                    {
                                        "dt": [
                                            ["text", "{bc}to become strong again"],
                                            ["vis", [{"t": "They had to resilient up after the storm."}]]
                                        ]
                                    }
                                ]
                            ]
                        ]
                    }
                ]
            }
        ]
    }
]


def test_parse_oxford_html_extracts_senses_cefr_audio_and_idioms():
    result = parse_oxford_html(SAMPLE_OXFORD_HTML, "resilient")
    assert result is not None
    assert result.word == "resilient"
    assert len(result.pronunciations) == 2
    uk_pron = next(p for p in result.pronunciations if p.accent == "UK")
    assert uk_pron.ipa == "/rɪˈzɪliənt/"
    assert uk_pron.audio_url.endswith("resilient__gb_1.mp3")

    assert len(result.senses) == 2
    s1 = result.senses[0]
    assert s1.part_of_speech == "adjective"
    assert s1.cefr_level == "B2"
    assert "able to feel better quickly" in s1.definition_en
    assert "young people are amazingly resilient" in s1.example_en
    assert "tough" in s1.synonyms
    assert "fragile" in s1.antonyms
    assert s1.topic == "Personal qualities"

    s2 = result.senses[1]
    assert s2.cefr_level == "C1"

    assert len(result.idioms) == 1
    assert result.idioms[0].phrase == "bounce back resiliently"
    assert "recover quickly" in result.idioms[0].definition_en


def test_parse_mw_thesaurus_extracts_sense_aligned_synonyms_and_antonyms():
    groups = parse_mw_thesaurus_json(SAMPLE_MW_THESAURUS_JSON, "resilient")
    assert len(groups) == 2
    assert "recover rapidly" in groups[0].sense_definition
    assert "adaptable" in groups[0].synonyms
    assert "fragile" in groups[0].antonyms
    assert "elastic" in groups[1].synonyms
    assert "rigid" in groups[1].antonyms


def test_parse_mw_dictionary_extracts_word_forms_and_fallback_senses():
    mw_dict = parse_mw_dictionary_json(SAMPLE_MW_DICTIONARY_JSON, "resilient")
    assert mw_dict is not None
    assert "resiliently" in mw_dict.word_forms
    assert "resilience" in mw_dict.word_forms
    assert len(mw_dict.senses) == 1
    assert "tending to recover" in mw_dict.senses[0].definition_en
    assert "children are often very resilient" in mw_dict.senses[0].example_en
    assert len(mw_dict.pronunciations) == 1
    assert "resili02.mp3" in mw_dict.pronunciations[0].audio_url


def test_merge_crawl_results_sets_correct_data_source_flags():
    oxford_res = parse_oxford_html(SAMPLE_OXFORD_HTML, "resilient")
    mw_thes = parse_mw_thesaurus_json(SAMPLE_MW_THESAURUS_JSON, "resilient")
    mw_dict = parse_mw_dictionary_json(SAMPLE_MW_DICTIONARY_JSON, "resilient")

    hybrid = merge_crawl_results("resilient", oxford_res, mw_thes, mw_dict)
    assert hybrid.status == "SUCCESS"
    assert hybrid.data_source == "OXFORD_MW_HYBRID"
    assert "resiliently" in hybrid.payload.word_forms
    assert len(hybrid.payload.senses) == 2
    assert len(hybrid.payload.mw_sense_syn_ant_groups) == 2

    oxford_only = merge_crawl_results("resilient", oxford_res, [], None)
    assert oxford_only.data_source == "OXFORD_CRAWLER"

    mw_only = merge_crawl_results("resilient", None, mw_thes, mw_dict)
    assert mw_only.data_source == "MERRIAM_WEBSTER_API"
    assert len(mw_only.payload.senses) == 1

    not_found = merge_crawl_results("nonexistentxyz", None, [], None)
    assert not_found.status == "NOT_FOUND"
    assert not_found.data_source == "NOT_FOUND"


def test_fastapi_health_endpoint():
    client = TestClient(app)
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ok"

