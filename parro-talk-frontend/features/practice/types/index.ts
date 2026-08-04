export type PracticeSessionStatus = 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export type PracticeQuestionType = 'FLASHCARD' | 'MULTIPLE_CHOICE' | 'LISTENING' | 'TYPING' | 'SENTENCE_FILL' | 'MATCH';

export type Sm2Rating = 'AGAIN' | 'HARD' | 'GOOD' | 'EASY';

export interface PracticeQuestion {
  userVocabularyId: string;
  word: string;
  displayWord: string;
  phonetic?: string;
  audioUrl?: string;
  partOfSpeech?: string;
  definition?: string;
  questionType: PracticeQuestionType;
  options?: string[];
  sentenceTemplate?: string;
}

export interface PracticeSession {
  sessionId: string;
  status: PracticeSessionStatus;
  startedAt: string;
  questions: PracticeQuestion[];
}

export interface AnswerSubmission {
  sessionId: string;
  userVocabularyId: string;
  answer?: string;
  rating?: Sm2Rating;
  timeSpentMs?: number;
}

export interface AnswerResult {
  correct: boolean;
  explanation: string;
  correctAnswer: string;
  xpEarned: number;
}

export interface PracticeResult {
  sessionId: string;
  totalQuestions: number;
  correctAnswers: number;
  accuracy: number;
  xpEarned: number;
  newMasteredWords: number;
  streak: number;
}

export interface PracticeStatistics {
  todayLearned: number;
  totalMastered: number;
  currentStreak: number;
  reviewDue: number;
  retentionRate: number;
}
