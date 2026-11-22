export type QuizMode = "MULTIPLE_CHOICE" | "FILL_IN_THE_BLANK" | "MIXED";

export type QuizQuestionType = "MULTIPLE_CHOICE" | "FILL_IN_THE_BLANK";

export type QuizAttemptStatus = "IN_PROGRESS" | "COMPLETED";

export type QuizGradingStatus = "CORRECT" | "PARTIAL" | "INCORRECT";

export interface QuizAnswerResult {
  questionId: string;
  userAnswer: string | null;
  selectedIndex: number | null;
  status: QuizGradingStatus;
  score: number;
  feedback: string;
  modelAnswer: string;
  correctIndex: number | null;
  explanationVn: string;
  evidenceSegmentId: string | null;
  evidenceText: string;
  evidenceStartTime: number;
  evidenceEndTime: number;
}

export interface LessonQuizQuestion {
  questionId: string;
  displayOrder: number;
  questionType: QuizQuestionType;
  questionText: string;
  options: string[] | null;
  answered: boolean;
  answerResult: QuizAnswerResult | null;
}

export interface LessonQuizAttemptSummary {
  attemptId: string;
  quizSetId: string;
  mode: QuizMode;
  status: QuizAttemptStatus;
  totalQuestions: number;
  answeredCount: number;
  correctCount: number;
  partialCount: number;
  overallScore: number;
  startedAt: string;
  completedAt: string | null;
}

export interface LessonQuizOverviewResponse {
  lessonId: string;
  recommendedQuestionCount: number;
  activeAttempt: LessonQuizAttemptSummary | null;
  cachedModes: QuizMode[];
  regenerateCooldownSeconds: Record<QuizMode, number>;
  history: LessonQuizAttemptSummary[];
}

export interface LessonQuizAttemptResponse {
  attemptId: string;
  lessonId: string;
  quizSetId: string;
  mode: QuizMode;
  status: QuizAttemptStatus;
  totalQuestions: number;
  answeredCount: number;
  correctCount: number;
  partialCount: number;
  overallScore: number;
  regenerateCooldownSeconds: number;
  startedAt: string;
  completedAt: string | null;
  questions: LessonQuizQuestion[];
}

export interface StartQuizAttemptRequest {
  mode: QuizMode;
  forceNew?: boolean;
}

export interface RegenerateQuizRequest {
  mode: QuizMode;
}

export interface SubmitQuizAnswerRequest {
  questionId: string;
  selectedIndex?: number | null;
  userAnswer?: string | null;
}
