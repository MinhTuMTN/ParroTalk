import assert from "node:assert/strict";
import test from "node:test";
import React from "react";
import { renderToString } from "react-dom/server";
import { WritingAssessmentResult } from "../WritingAssessmentResult";
import type { GrammarValidationResult } from "../../types";

const baseProps = {
  userDraft: "She go to school yesterday.",
  isEn: false,
  copiedCorrected: false,
  onCopyCorrected: () => {},
  onApplyCorrected: () => {},
  onInsertTerm: () => {},
};

test("renders overall score and all four assessment criteria with progress bars and Vietnamese feedback", () => {
  const validation: GrammarValidationResult = {
    valid: false,
    status: "NEEDS_REVIEW",
    summaryVn: "Câu đúng ý chính nhưng cần sửa thì quá khứ.",
    score: 78,
    aiReviewed: true,
    reviewSource: "AI",
    correctedSentence: "She went to school yesterday.",
    issues: [
      {
        ruleId: "AI_GRAMMAR",
        type: "GRAMMAR",
        original: "go",
        suggestion: "went",
        messageVn: "Cần chia quá khứ đơn 'went' với 'yesterday'.",
        offset: 4,
        length: 2,
        replacements: ["went"],
      },
    ],
    usedTerms: ["yesterday"],
    missingTerms: ["public transport"],
    assessmentCriteria: [
      {
        criterion: "TASK_FULFILLMENT",
        nameEn: "Task Fulfillment & Idea Development",
        labelVn: "Hoàn thành yêu cầu và phát triển ý",
        score: 85,
        weight: 0.25,
        feedbackVn: "Truyền đạt rõ ràng ý chính.",
      },
      {
        criterion: "COHERENCE_COHESION",
        nameEn: "Coherence & Cohesion",
        labelVn: "Tính mạch lạc và liên kết",
        score: 82,
        weight: 0.25,
        feedbackVn: "Câu đơn mạch lạc, rõ ràng.",
      },
      {
        criterion: "LEXICAL_RESOURCE",
        nameEn: "Lexical Resource",
        labelVn: "Vốn từ vựng",
        score: 76,
        weight: 0.25,
        feedbackVn: "Từ vựng phù hợp trình độ.",
      },
      {
        criterion: "GRAMMATICAL_RANGE_ACCURACY",
        nameEn: "Grammatical Range & Accuracy",
        labelVn: "Độ đa dạng và chính xác ngữ pháp",
        score: 68,
        weight: 0.25,
        feedbackVn: "Cần chú ý chia động từ quá khứ.",
      },
    ],
  };

  const html = renderToString(
    React.createElement(WritingAssessmentResult, {
      ...baseProps,
      validation,
    })
  );

  assert.match(html, /78\/100/);
  assert.match(html, /Task Fulfillment &amp; Idea Development|Task Fulfillment & Idea Development/);
  assert.match(html, /Hoàn thành yêu cầu và phát triển ý/);
  assert.match(html, /Coherence &amp; Cohesion|Coherence & Cohesion/);
  assert.match(html, /Tính mạch lạc và liên kết/);
  assert.match(html, /Lexical Resource/);
  assert.match(html, /Vốn từ vựng/);
  assert.match(html, /Grammatical Range &amp; Accuracy|Grammatical Range & Accuracy/);
  assert.match(html, /Độ đa dạng và chính xác ngữ pháp/);
  assert.match(html, /85\/100/);
  assert.match(html, /82\/100/);
  assert.match(html, /76\/100/);
  assert.match(html, /68\/100/);
  assert.match(html, /Truyền đạt rõ ràng ý chính\./);
  assert.match(html, /She went to school yesterday\./);
  assert.match(html, /yesterday/);
  assert.match(html, /public transport/);
  assert.match(html, /role="progressbar"/);
});

test("renders legacy AI result without assessmentCriteria gracefully and does not fabricate scores", () => {
  const legacyValidation: GrammarValidationResult = {
    valid: true,
    status: "APPROVED",
    summaryVn: "Câu tự nhiên và đúng ngữ pháp.",
    score: 92,
    correctedSentence: "She went to school yesterday.",
    issues: [],
    usedTerms: ["yesterday"],
    missingTerms: [],
  };

  const html = renderToString(
    React.createElement(WritingAssessmentResult, {
      ...baseProps,
      validation: legacyValidation,
    })
  );

  assert.match(html, /92\/100/);
  assert.match(html, /She went to school yesterday\./);
  assert.match(html, /Kết quả đánh giá theo phiên bản cũ|Legacy assessment format/);
  assert.doesNotMatch(html, /role="progressbar"/);
});

test("renders rule-only fallback notice when AI review was unavailable", () => {
  const ruleOnlyValidation: GrammarValidationResult = {
    valid: true,
    status: "APPROVED",
    summaryVn: "Cấu trúc cơ bản hợp lệ. (AI review unavailable.)",
    issues: [],
    aiReviewed: false,
    reviewSource: "RULE_ONLY",
  };

  const html = renderToString(
    React.createElement(WritingAssessmentResult, {
      ...baseProps,
      validation: ruleOnlyValidation,
    })
  );

  assert.match(html, /Đang kiểm tra theo quy tắc cơ bản|Basic rule-based check only/);
  assert.doesNotMatch(html, /role="progressbar"/);
});
