import assert from "node:assert/strict";
import test from "node:test";
import React from "react";
import { renderToString } from "react-dom/server";
import QuizFeedbackCard from "../QuizFeedbackCard.tsx";
import QuizQuestionView from "../QuizQuestionView.tsx";
import QuizSetupPanel from "../QuizSetupPanel.tsx";
import type {
  LessonQuizAttemptResponse,
  LessonQuizOverviewResponse,
  QuizAnswerResult,
} from "../../../types/quiz.ts";

function renderHtml(element: React.ReactElement): string {
  return renderToString(element).replace(/<!--\s*-->/g, "");
}

test("QuizQuestionView hides transcript evidence and feedback completely when answered is false", () => {
  const attempt: LessonQuizAttemptResponse = {
    attemptId: "att-1",
    lessonId: "lesson-1",
    quizSetId: "set-1",
    mode: "MIXED",
    status: "IN_PROGRESS",
    totalQuestions: 2,
    answeredCount: 0,
    correctCount: 0,
    partialCount: 0,
    overallScore: 0,
    regenerateCooldownSeconds: 0,
    startedAt: "2026-11-22T02:00:00Z",
    completedAt: null,
    questions: [
      {
        questionId: "q-1",
        displayOrder: 1,
        questionType: "MULTIPLE_CHOICE",
        questionText: "What does the speaker recommend doing first?",
        options: [
          "Review the outline",
          "Skip the intro",
          "Close the notes",
          "Wait until Friday",
        ],
        answered: false,
        answerResult: null,
      },
      {
        questionId: "q-2",
        displayOrder: 2,
        questionType: "FILL_IN_THE_BLANK",
        questionText: "The meeting starts at ___ every Monday.",
        options: null,
        answered: false,
        answerResult: null,
      },
    ],
  };

  const html = renderHtml(
    React.createElement(QuizQuestionView, {
      attempt,
      currentIndex: 0,
      onSelectIndex: () => {},
      onSubmitAnswer: () => {},
      onCompleteAttempt: () => {},
    }),
  );

  assert.match(html, /What does the speaker recommend doing first\?/);
  assert.match(html, /Review the outline/);
  assert.doesNotMatch(html, /Trích đoạn dẫn chứng từ bài nghe/);
  assert.doesNotMatch(html, /Nghe đoạn dẫn chứng/);
  assert.doesNotMatch(html, /Nhận xét AI/);
  assert.doesNotMatch(html, /Đáp án mẫu/);
});

test("QuizFeedbackCard renders CORRECT, PARTIAL, and INCORRECT badges, score, explanationVn, evidenceText, and timestamp button", () => {
  const baseResult: QuizAnswerResult = {
    questionId: "q-1",
    userAnswer: "nine o'clock",
    selectedIndex: null,
    status: "CORRECT",
    score: 100,
    feedback: "Bạn đã nghe chính xác thời gian bắt đầu cuộc họp.",
    modelAnswer: "9:00 AM",
    correctIndex: null,
    explanationVn: "Người nói nhắc rõ cuộc họp diễn ra lúc 9 giờ sáng.",
    evidenceSegmentId: "seg-3",
    evidenceText: "Our weekly sync always begins at nine o'clock sharp.",
    evidenceStartTime: 65,
    evidenceEndTime: 78,
  };

  const correctHtml = renderHtml(
    React.createElement(QuizFeedbackCard, {
      answerResult: baseResult,
      onPlayEvidence: () => {},
    }),
  );

  assert.match(correctHtml, /Đúng/);
  assert.match(correctHtml, /100\/100 điểm/);
  assert.match(correctHtml, /Bạn đã nghe chính xác thời gian bắt đầu cuộc họp\./);
  assert.match(correctHtml, /9:00 AM/);
  assert.match(correctHtml, /Người nói nhắc rõ cuộc họp diễn ra lúc 9 giờ sáng\./);
  assert.match(
    correctHtml,
    /Our weekly sync always begins at nine o&#x27;clock sharp\.|Our weekly sync always begins at nine o'clock sharp\./,
  );
  assert.match(correctHtml, /Nghe đoạn dẫn chứng \(01:05 - 01:18\)/);

  const partialHtml = renderHtml(
    React.createElement(QuizFeedbackCard, {
      answerResult: {
        ...baseResult,
        status: "PARTIAL",
        score: 65,
      },
      onPlayEvidence: () => {},
    }),
  );
  assert.match(partialHtml, /Gần đúng/);
  assert.match(partialHtml, /65\/100 điểm/);

  const incorrectHtml = renderHtml(
    React.createElement(QuizFeedbackCard, {
      answerResult: {
        ...baseResult,
        status: "INCORRECT",
        score: 0,
      },
      onPlayEvidence: () => {},
    }),
  );
  assert.match(incorrectHtml, /Chưa đúng/);
  assert.match(incorrectHtml, /0\/100 điểm/);
});

test("QuizSetupPanel renders resume banner, cached badge, and disabled regenerate button with cooldown countdown", () => {
  const overview: LessonQuizOverviewResponse = {
    lessonId: "lesson-1",
    recommendedQuestionCount: 6,
    activeAttempt: {
      attemptId: "att-active",
      quizSetId: "set-1",
      mode: "FILL_IN_THE_BLANK",
      status: "IN_PROGRESS",
      totalQuestions: 6,
      answeredCount: 2,
      correctCount: 2,
      partialCount: 0,
      overallScore: 100,
      startedAt: "2026-11-22T01:50:00Z",
      completedAt: null,
    },
    cachedModes: ["MULTIPLE_CHOICE", "MIXED"],
    regenerateCooldownSeconds: {
      MULTIPLE_CHOICE: 0,
      FILL_IN_THE_BLANK: 45,
      MIXED: 0,
    },
    history: [
      {
        attemptId: "att-old",
        quizSetId: "set-0",
        mode: "MULTIPLE_CHOICE",
        status: "COMPLETED",
        totalQuestions: 6,
        answeredCount: 6,
        correctCount: 5,
        partialCount: 0,
        overallScore: 83,
        startedAt: "2026-11-21T10:00:00Z",
        completedAt: "2026-11-21T10:05:00Z",
      },
    ],
  };

  const html = renderHtml(
    React.createElement(QuizSetupPanel, {
      overview,
      selectedMode: "FILL_IN_THE_BLANK",
      onSelectMode: () => {},
      onStartQuiz: () => {},
      onResumeAttempt: () => {},
      onRegenerateQuiz: () => {},
      onReviewHistory: () => {},
    }),
  );

  assert.match(html, /Tiếp tục bài làm dở/);
  assert.match(html, /2\/6/);
  assert.match(html, /Đã sẵn sàng/);
  assert.match(html, /Tạo lại sau 45s/);
  assert.match(html, /disabled=""/);
  assert.match(html, /83\/100/);
  assert.match(html, /Xem lại/);
});
