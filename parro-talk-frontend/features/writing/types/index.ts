export type WritingMode = "SUGGESTION" | "GUIDANCE";
export type DifficultyScale = "CEFR" | "IELTS";
export type WritingItemType =
  | "VOCABULARY"
  | "EXPRESSION"
  | "COLLOCATION"
  | "SENTENCE_PATTERN"
  | "COMPLETE_SENTENCE";
export type WritingItemValidationStatus =
  | "PENDING"
  | "VALIDATING"
  | "APPROVED"
  | "REJECTED"
  | "NEEDS_REVIEW";

export interface GrammarIssue {
  ruleId: string;
  message: string;
  messageVn: string;
  offset: number;
  length: number;
  replacements: string[];
}

export interface GrammarValidationResult {
  valid: boolean;
  status: WritingItemValidationStatus;
  summaryVn: string;
  issues: GrammarIssue[];
}

export interface WritingLexicalItem {
  id: string;
  itemType: WritingItemType;
  text: string;
  normalizedText?: string;
  partOfSpeech?: string;
  cefrLevel?: string;
  meaningVn?: string;
  explanationVn?: string;
  usageContextVn?: string;
  exampleEn?: string;
  exampleVn?: string;
  inDictionary: boolean;
  linkedVocabularyId?: string;
  linkedSenseId?: string;
  dictionaryWord?: string;
  validationStatus: WritingItemValidationStatus;
}

export interface WritingPatternItem {
  id: string;
  pattern: string;
  meaningVn?: string;
  explanationVn?: string;
  usageContextVn?: string;
  validationStatus: WritingItemValidationStatus;
}

export interface WritingCompleteSentenceItem {
  id: string;
  tone?: string;
  sentenceEn: string;
  translationVn?: string;
  grammarBreakdownVn?: string;
  whyItWorksVn?: string;
  validationStatus: WritingItemValidationStatus;
  grammarCheck?: GrammarValidationResult;
}

export interface WritingSessionResponse {
  id: string;
  inputText: string;
  inputLanguage: string;
  mode: WritingMode;
  scaleType: DifficultyScale;
  targetLevel: string;
  coreIdeaSummaryVn?: string;
  combinationGuideVn?: string;
  userDraft?: string;
  draftValidation?: GrammarValidationResult;
  createdAt?: string;
  vocabulary: WritingLexicalItem[];
  expressions: WritingLexicalItem[];
  collocations: WritingLexicalItem[];
  sentencePatterns: WritingPatternItem[];
  completeSentences: WritingCompleteSentenceItem[];
}

export interface WritingSessionSummary {
  id: string;
  inputText: string;
  inputLanguage: string;
  mode: WritingMode;
  scaleType: DifficultyScale;
  targetLevel: string;
  coreIdeaSummaryVn?: string;
  hasUserDraft: boolean;
  createdAt: string;
}

export interface WritingAnalyzeRequest {
  inputText: string;
  mode: WritingMode;
  scaleType: DifficultyScale;
  targetLevel: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

