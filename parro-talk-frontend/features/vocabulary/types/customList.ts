export type VocabularyDataSource =
  | "IMPORT_JSON"
  | "OXFORD_CRAWLER"
  | "MERRIAM_WEBSTER_API"
  | "OXFORD_MW_HYBRID"
  | "AI_GENERATED"
  | "ADMIN_MANUAL";

export type QuestionGenerationSource =
  | "PIPELINE_BATCH"
  | "USER_LIST_FINALIZE_AI"
  | "USER_LIST_MORE_AI"
  | "ADMIN_MANUAL";

export type McqQuestionCategory =
  | "CONTEXTUAL_CLOZE"
  | "SYNONYM_IN_CONTEXT"
  | "ANTONYM_IN_CONTEXT"
  | "DEFINITION_MATCH"
  | "COLLOCATION_PREPOSITION"
  | "WORD_FORM"
  | "IDIOM_PHRASAL_VERB"
  | "USAGE_ERROR_DETECTION";

export type VocabularyListVisibility = "PRIVATE" | "SHARED_LINK";

export type VocabularyListStatus =
  | "DRAFT"
  | "GENERATING_QUESTIONS"
  | "ACTIVE"
  | "ERROR";

export type VocabularyCrawlStatus =
  | "RESOLVED_FROM_DB"
  | "PENDING"
  | "PROCESSING"
  | "COMPLETED"
  | "NOT_FOUND"
  | "FAILED";

export interface SenseItemDto {
  id: string;
  partOfSpeech?: string;
  cefrLevel?: string;
  definitionEn: string;
  definitionVn?: string;
  examplesEn?: string[];
  examplesVn?: string[];
  synonyms?: string[];
  antonyms?: string[];
  collocations?: string[];
  dataSource?: VocabularyDataSource;
  selected?: boolean;
  questionCount?: number;
}

export interface IdiomItemDto {
  id: string;
  phrase: string;
  definitionEn?: string;
  definitionVn?: string;
  meaningEn?: string;
  meaningVn?: string;
  exampleEn?: string;
  exampleVn?: string;
  dataSource?: VocabularyDataSource;
  selected?: boolean;
}

export interface PhrasalVerbItemDto {
  id: string;
  phrase: string;
  definitionEn?: string;
  definitionVn?: string;
  meaningEn?: string;
  meaningVn?: string;
  exampleEn?: string;
  exampleVn?: string;
  dataSource?: VocabularyDataSource;
  selected?: boolean;
}

export interface VocabularyListItemResponse {
  id: string;
  vocabularyId?: string;
  rawInputWord?: string;
  rawWord?: string;
  normalizedWord?: string;
  word?: string;
  commonMeaningVn?: string;
  phoneticUk?: string;
  phoneticUs?: string;
  audioUkUrl?: string;
  audioUsUrl?: string;
  dataSource?: VocabularyDataSource;
  crawlStatus: VocabularyCrawlStatus;
  crawlErrorMessage?: string;
  selectedSenseIds: string[];
  selectedIdiomIds: string[];
  selectedPhrasalVerbIds: string[];
  availableSenses: SenseItemDto[];
  availableIdioms: IdiomItemDto[];
  availablePhrasalVerbs: PhrasalVerbItemDto[];
}

export interface ShareEntryDto {
  id: string;
  sharedWithEmail: string;
  sharedWithName?: string;
  permission: string;
  createdAt: string;
}

export interface VocabularyListSummaryResponse {
  id: string;
  title: string;
  description?: string;
  targetCefr?: string;
  visibility: VocabularyListVisibility;
  status: VocabularyListStatus;
  shareToken?: string;
  ownerName?: string;
  ownerEmail?: string;
  isOwner?: boolean;
  owner: boolean;
  wordCount: number;
  selectedSenseCount?: number;
  updatedAt?: string;
}

export interface VocabularyListDetailResponse {
  id: string;
  title: string;
  description?: string;
  targetCefr?: string;
  visibility: VocabularyListVisibility;
  status: VocabularyListStatus;
  shareToken?: string;
  ownerName?: string;
  ownerEmail?: string;
  isOwner?: boolean;
  owner: boolean;
  items: VocabularyListItemResponse[];
  shares: ShareEntryDto[];
  updatedAt?: string;
}

export interface CustomListMcqResponse {
  id: string;
  senseId?: string;
  idiomId?: string;
  phrasalVerbId?: string;
  targetWord: string;
  partOfSpeech?: string;
  cefrLevel?: string;
  questionCategory: McqQuestionCategory;
  generationSource: QuestionGenerationSource;
  questionText: string;
  options: string[];
  correctAnswer: string;
  completedSentence?: string;
  explanationEn?: string;
  explanationVn?: string;
}

export interface UserNotificationDto {
  id: string;
  type:
    | "CRAWL_COMPLETED"
    | "CRAWL_NOT_FOUND"
    | "LIST_QUESTIONS_READY"
    | "LIST_SHARED_WITH_YOU";
  title: string;
  message: string;
  referenceId?: string;
  payloadJson?: string;
  isRead: boolean;
  createdAt: string;
}
