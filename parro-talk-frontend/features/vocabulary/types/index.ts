export interface VocabularyTopicDto {
  id: string;
  name: string;
  icon: string;
  description?: string;
  wordCount: number;
}

export interface VocabularySummaryDto {
  id: string;
  word: string;
  phonetic: string;
  cefrLevel: string;
  partOfSpeech: string;
  commonMeaningVi: string;
  topic?: string;
  audioUsUrl?: string;
  audioUkUrl?: string;
}

export interface VocabularyDetailDto extends VocabularySummaryDto {
  definitions: string[];
  examples: string[];
  idioms: string[];
  collocations: string[];
  phrasalVerbs: string[];
  synonyms: string[];
  antonyms: string[];
  isSaved?: boolean;
}

export interface VocabularyReportRequestDto {
  reportType: string;
  reason: string;
  description: string;
}

export interface PracticeQuestionDto {
  id: string;
  questionType: 'MULTIPLE_CHOICE' | 'FLASHCARD' | 'FILL_IN_BLANKS';
  prompt: string;
  options?: string[];
  correctAnswer: string;
  wordHint?: string;
  charCount?: number;
  audioUrl?: string;
  explanation?: string;
  wordId: string;
}