import test from "node:test";
import assert from "node:assert/strict";
import {
  getDefaultSelectedSenseIds,
  formatQuestionCategoryLabel,
  formatGenerationSourceBadge,
  formatDataSourceBadge,
  resolveIsOwner,
  resolveItemDisplayWord,
} from "../customListUtils.ts";

test("getDefaultSelectedSenseIds auto-selects A1 to B2 senses and falls back to first sense if only C1/C2", () => {
  const senses = [
    { id: "s1", cefrLevel: "B1", partOfSpeech: "verb", definitionEn: "def 1" },
    { id: "s2", cefrLevel: "B2", partOfSpeech: "verb", definitionEn: "def 2" },
    { id: "s3", cefrLevel: "C1", partOfSpeech: "noun", definitionEn: "def 3" },
  ];

  const selected = getDefaultSelectedSenseIds(senses);
  assert.deepEqual(selected, ["s1", "s2"]);

  const c1Only = [
    { id: "s9", cefrLevel: "C1", partOfSpeech: "adjective", definitionEn: "def 9" },
  ];
  assert.deepEqual(getDefaultSelectedSenseIds(c1Only), ["s9"]);
});

test("formatQuestionCategoryLabel returns readable label for all 8 MCQ categories", () => {
  assert.equal(formatQuestionCategoryLabel("CONTEXTUAL_CLOZE"), "Điền từ theo ngữ cảnh");
  assert.equal(formatQuestionCategoryLabel("SYNONYM_IN_CONTEXT"), "Từ đồng nghĩa");
  assert.equal(formatQuestionCategoryLabel("ANTONYM_IN_CONTEXT"), "Từ trái nghĩa");
  assert.equal(formatQuestionCategoryLabel("DEFINITION_MATCH"), "Định nghĩa");
  assert.equal(formatQuestionCategoryLabel("COLLOCATION_PREPOSITION"), "Collocation & Giới từ");
  assert.equal(formatQuestionCategoryLabel("WORD_FORM"), "Word Family / Từ loại");
  assert.equal(formatQuestionCategoryLabel("IDIOM_PHRASAL_VERB"), "Idiom & Phrasal Verb");
  assert.equal(formatQuestionCategoryLabel("USAGE_ERROR_DETECTION"), "Ứng dụng câu chuẩn");
});

test("formatGenerationSourceBadge and formatDataSourceBadge distinguish AI/Crawler sources", () => {
  assert.equal(formatGenerationSourceBadge("USER_LIST_FINALIZE_AI"), "Chốt List AI");
  assert.equal(formatGenerationSourceBadge("USER_LIST_MORE_AI"), "Tạo thêm AI");
  assert.equal(formatGenerationSourceBadge("PIPELINE_BATCH"), "Kho câu hỏi chung");
  assert.equal(formatDataSourceBadge("OXFORD_MW_HYBRID"), "Oxford + Merriam-Webster");
  assert.equal(formatDataSourceBadge("OXFORD_CRAWLER"), "Oxford Dictionary");
});

test("resolveIsOwner handles isOwner, owner property and undefined gracefully", () => {
  assert.equal(resolveIsOwner({ isOwner: true }), true);
  assert.equal(resolveIsOwner({ owner: true }), true);
  assert.equal(resolveIsOwner({ isOwner: false }), false);
  assert.equal(resolveIsOwner({ isOwner: false, owner: true }), false);
  assert.equal(resolveIsOwner(null), false);
  assert.equal(resolveIsOwner(undefined), false);
});

test("resolveItemDisplayWord falls back from word -> normalizedWord -> rawWord -> rawInputWord", () => {
  assert.equal(resolveItemDisplayWord({ word: "apple" }), "apple");
  assert.equal(resolveItemDisplayWord({ normalizedWord: "banana" }), "banana");
  assert.equal(resolveItemDisplayWord({ rawWord: "Cherry" }), "Cherry");
  assert.equal(resolveItemDisplayWord({ rawInputWord: "durian" }), "durian");
  assert.equal(resolveItemDisplayWord(null), "");
});

