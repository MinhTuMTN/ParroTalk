export interface WordSenseItem {
  id: string;
  orderNumber: string; // "01", "02", etc.
  title: string; // e.g. "A problem or matter"
  partOfSpeech: string; // "noun"
  countability?: string; // "countable"
  cefrLevel: string; // "B1"
  topicTag: string; // "Business", "Media", "Administration", "Finance"
  definitionEn: string;
  definitionVn: string;
  usageContextNote?: string; // "At work · discussing concerns and finding solutions"
  inContext: {
    english: string;
    vietnamese: string;
  };
  additionalExamples?: { english: string; vietnamese: string }[];
  collocations?: string[];
  synonyms?: string[];
}

export interface WordPhrasalVerbItem {
  phrasalVerb: string;
  register?: string; // "formal / literary"
  definitionEn: string;
  definitionVn: string;
  exampleEn: string;
  exampleVn: string;
}

export interface WordIdiomItem {
  idiom: string;
  relatedSenseNote?: string; // "Related to meaning 01 · a problem or matter"
  definitionEn: string;
  definitionVn: string;
  exampleEn: string;
  exampleVn: string;
}

export interface RichVocabularyDetailData {
  id: string;
  word: string;
  isNew?: boolean;
  partOfSpeechSummary: string; // "noun / verb · B1-C1 across senses"
  vietnameseSummary: string; // "vấn đề; số báo; cấp, phát hành"
  pronunciations: {
    uk: { ipa: string; audioUrl?: string };
    us: { ipa: string; audioUrl?: string };
  };
  topics: string[]; // ["Business", "Media", "Administration", "Finance"]
  senses: WordSenseItem[];
  phrasalVerbs: WordPhrasalVerbItem[];
  idioms: WordIdiomItem[];
}
