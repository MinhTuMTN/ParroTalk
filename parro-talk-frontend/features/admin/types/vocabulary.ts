export interface AdminDefinition {
  id?: string;
  definition: string;
  englishDefinition?: string;
  vietnameseDefinition?: string;
  displayOrder: number;
}

export interface AdminExample {
  id?: string;
  sentence: string;
  translation?: string;
  displayOrder: number;
}

export interface AdminRelation {
  id?: string;
  relationType: 'SYNONYM' | 'ANTONYM' | 'COLLOCATION' | 'IDIOM' | 'PHRASAL_VERB' | 'WORD_FORM';
  relatedWord: string;
  displayOrder: number;
}

export interface AdminVocabularyRequest {
  word: string;
  ipaUk?: string;
  ipaUs?: string;
  audioUk?: string;
  audioUs?: string;
  cefrLevel?: string;
  frequencyRank?: number;
  partOfSpeech?: string;
  imageUrl?: string;
  notes?: string;
  source?: string;
  status?: string;

  definitions: AdminDefinition[];
  examples: AdminExample[];
  relations: AdminRelation[];
  categoryIds: string[];
  tags: string[];
}

export interface AdminVocabularyResponse {
  id: string;
  word: string;
  ipaUk?: string;
  ipaUs?: string;
  audioUk?: string;
  audioUs?: string;
  cefrLevel?: string;
  frequencyRank?: number;
  partOfSpeech?: string;
  imageUrl?: string;
  notes?: string;
  source?: string;
  status?: string;
  createdAt: string;
  updatedAt: string;

  definitions: AdminDefinition[];
  examples: AdminExample[];
  relations: AdminRelation[];
  categories: string[];
  tags: string[];
}

export interface AdminVocabularyListDto {
  id: string;
  word: string;
  partOfSpeech?: string;
  cefrLevel?: string;
  definitionsCount: number;
  examplesCount: number;
  hasAudio: boolean;
  status: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
