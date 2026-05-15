export interface Sentence {
  id: string;
  audioUrl: string;
  text: string;
}

export interface PracticeResult {
  sentenceId: string;
  userInput: string;
  isCorrect: boolean;
  score: number;
  tokens: FeedbackToken[];
}

export interface FeedbackToken {
  text: string;
  isCorrect: boolean;
  expected?: string;
}

