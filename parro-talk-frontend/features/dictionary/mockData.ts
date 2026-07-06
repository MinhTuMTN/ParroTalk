export type CefrLevel = "A1" | "A2" | "B1" | "B2" | "C1" | "C2";
export type WordStatus = "New" | "Learning" | "Mastered";

export type WordSense = {
  definitionEn: string;
  translationVi: string;
  exampleEn: string;
  exampleVi: string;
};

export type DictionaryWord = {
  id: string;
  lemma: string;
  phonetic: string;
  partOfSpeech: string;
  cefrLevel: CefrLevel;
  translationVi: string;
  definitionEn: string;
  category: string;
  status: WordStatus;
  synonyms: string[];
  antonyms: string[];
  senses: WordSense[];
};

export type WordSet = {
  id: string;
  name: string;
  description: string;
  totalWords: number;
  levels: CefrLevel[];
  learned: number;
  dueToday: number;
};

export const wordSets: WordSet[] = [
  {
    id: "oxford-3000",
    name: "Oxford 3000",
    description: "Essential English words every learner should know.",
    totalWords: 3000,
    levels: ["A1", "A2", "B1", "B2"],
    learned: 124,
    dueToday: 18,
  },
  {
    id: "oxford-5000",
    name: "Oxford 5000",
    description: "Upper-intermediate and advanced words for confident English.",
    totalWords: 5000,
    levels: ["B2", "C1"],
    learned: 42,
    dueToday: 5,
  },
  {
    id: "saved-words",
    name: "Saved Words",
    description: "Words you saved while practicing dictation lessons.",
    totalWords: 214,
    levels: ["A1", "A2", "B1", "B2", "C1"],
    learned: 89,
    dueToday: 12,
  },
];

export const dictionaryWords: DictionaryWord[] = [
  {
    id: "journey",
    lemma: "journey",
    phonetic: "/ˈdʒɜːrni/",
    partOfSpeech: "noun",
    cefrLevel: "A2",
    translationVi: "chuyến đi",
    definitionEn: "An act of travelling from one place to another.",
    category: "Travel",
    status: "Learning",
    synonyms: ["trip", "travel", "voyage"],
    antonyms: [],
    senses: [
      {
        definitionEn: "An act of travelling from one place to another.",
        translationVi: "chuyến đi",
        exampleEn: "They went on a long journey across Europe.",
        exampleVi: "Họ đã có một chuyến đi dài xuyên châu Âu.",
      },
      {
        definitionEn: "A long process of personal change or development.",
        translationVi: "hành trình phát triển",
        exampleEn: "Learning English is a journey, not a race.",
        exampleVi: "Học tiếng Anh là một hành trình, không phải một cuộc đua.",
      },
    ],
  },
  {
    id: "improve",
    lemma: "improve",
    phonetic: "/ɪmˈpruːv/",
    partOfSpeech: "verb",
    cefrLevel: "A2",
    translationVi: "cải thiện",
    definitionEn: "To become better, or to make something better.",
    category: "Education",
    status: "New",
    synonyms: ["develop", "upgrade", "enhance"],
    antonyms: ["worsen"],
    senses: [
      {
        definitionEn: "To become better, or to make something better.",
        translationVi: "cải thiện",
        exampleEn: "Dictation helps improve your listening accuracy.",
        exampleVi: "Nghe chép chính tả giúp cải thiện độ chính xác khi nghe.",
      },
    ],
  },
  {
    id: "accurate",
    lemma: "accurate",
    phonetic: "/ˈækjərət/",
    partOfSpeech: "adjective",
    cefrLevel: "B1",
    translationVi: "chính xác",
    definitionEn: "Correct and true in every detail.",
    category: "Academic",
    status: "Learning",
    synonyms: ["correct", "exact", "precise"],
    antonyms: ["inaccurate", "wrong"],
    senses: [
      {
        definitionEn: "Correct and true in every detail.",
        translationVi: "chính xác",
        exampleEn: "The transcript has accurate word-level timestamps.",
        exampleVi: "Bản transcript có timestamp từng từ chính xác.",
      },
    ],
  },
  {
    id: "context",
    lemma: "context",
    phonetic: "/ˈkɑːntekst/",
    partOfSpeech: "noun",
    cefrLevel: "B1",
    translationVi: "ngữ cảnh",
    definitionEn: "The situation in which something happens or is understood.",
    category: "Communication",
    status: "Mastered",
    synonyms: ["situation", "background"],
    antonyms: [],
    senses: [
      {
        definitionEn: "The situation in which something happens or is understood.",
        translationVi: "ngữ cảnh",
        exampleEn: "The correct meaning depends on the context of the sentence.",
        exampleVi: "Nghĩa đúng phụ thuộc vào ngữ cảnh của câu.",
      },
    ],
  },
];

export const categories = ["Daily Life", "Travel", "Business", "Academic", "Technology", "Media", "Education", "Communication"];
export const cefrLevels: CefrLevel[] = ["A1", "A2", "B1", "B2", "C1", "C2"];

export const practiceQuestions = [
  {
    id: 1,
    mode: "LISTEN_CHOOSE_WORD",
    prompt: "Choose the word you hear",
    correctAnswer: "journey",
    options: ["journey", "journal", "join", "joy"],
    definitionEn: "An act of travelling from one place to another.",
    translationVi: "chuyến đi",
  },
  {
    id: 2,
    mode: "CHOOSE_WORD_FROM_MEANING",
    prompt: "Choose the matching English word",
    correctAnswer: "accurate",
    options: ["accurate", "active", "actual", "ancient"],
    definitionEn: "Correct and true in every detail.",
    translationVi: "chính xác",
  },
];

export function getWordById(id: string) {
  return dictionaryWords.find((word) => word.id === id) ?? dictionaryWords[0];
}

export function getSetById(id: string) {
  return wordSets.find((set) => set.id === id) ?? wordSets[0];
}
