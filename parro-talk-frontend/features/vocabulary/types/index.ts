export interface VocabularyTopicDto {
  id: string;
  name: string;
  icon: string;
  description?: string;
  wordCount: number;
  learnedWordCount?: number;
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

export interface StructuredSenseDto {
  id?: string;
  orderNumber?: string;
  title?: string;
  partOfSpeech?: string;
  countability?: string;
  cefrLevel?: string;
  topicTag?: string;
  definitionEn: string;
  definitionVn?: string;
  usageContextNote?: string;
  exampleEn?: string;
  exampleVn?: string;
  synonyms?: string[];
  collocations?: string[];
}

export interface StructuredPhrasalVerbDto {
  phrasalVerb: string;
  register?: string;
  definitionEn: string;
  definitionVn?: string;
  exampleEn?: string;
  exampleVn?: string;
}

export interface StructuredIdiomDto {
  idiom: string;
  relatedSenseNote?: string;
  definitionEn: string;
  definitionVn?: string;
  exampleEn?: string;
  exampleVn?: string;
}

export interface VocabularyDetailDto extends VocabularySummaryDto {
  definitions: string[];
  examples: string[];
  idioms: string[];
  collocations: string[];
  phrasalVerbs: string[];
  synonyms: string[];
  antonyms: string[];
  senses?: StructuredSenseDto[];
  structuredPhrasalVerbs?: StructuredPhrasalVerbDto[];
  structuredIdioms?: StructuredIdiomDto[];
  isSaved?: boolean;
}

export interface VocabularyReportRequestDto {
  reportType: string;
  reason: string;
  description: string;
}

export interface PracticeQuestionDto {
  id: string;
  questionType: 'MULTIPLE_CHOICE' | 'FLASHCARD' | 'FILL_IN_BLANKS' | 'TYPE_WORD' | 'LISTEN_TYPE';
  prompt: string;
  options?: string[];
  correctAnswer: string;
  wordHint?: string;
  charCount?: number;
  audioUrl?: string;
  explanation?: string;
  wordId: string;
  definition?: string;
  synonymOrClue?: string;
  firstLetter?: string;
  exampleSentence?: string;
  sentenceWithBlank?: string;
  fullSentence?: string;
  easySentence?: string;
  clues?: string[];
}