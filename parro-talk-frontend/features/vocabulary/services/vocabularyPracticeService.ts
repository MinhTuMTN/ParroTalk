import axiosInstance from "@/lib/axios";

export interface LearnWordItemDto {
  id: string;
  word: string;
  phonetic?: string;
  phoneticUs?: string;
  phoneticUk?: string;
  audioUrl?: string;
  audioUrlUs?: string;
  audioUrlUk?: string;
  audioUsUrl?: string;
  audioUkUrl?: string;
  partOfSpeech?: string;
  cefrLevel?: string;
  topic?: string;
  definitionEn: string;
  definitionVn: string;
  exampleEn?: string;
  exampleVn?: string;
  synonyms?: string[];
  antonyms?: string[];
  collocations?: string[];
}

export interface MultipleChoiceItemDto {
  id: string;
  question: string;
  targetWord: string;
  options: string[];
  correctAnswer: string;
  correctIndex?: number;
  phonetic?: string;
  partOfSpeech?: string;
  sentence?: string;
  explanation?: string;
  audioUrl?: string;
}

export interface TypeWordItemDto {
  id: string;
  targetWord: string;
  promptDefinition: string;
  definitionEn?: string;
  definitionVn?: string;
  firstLetter?: string;
  characterCount?: number;
  partOfSpeech?: string;
  phonetic?: string;
  audioUrl?: string;
  exampleWithMask?: string;
  exampleSentence?: string;
}

export interface FillBlankItemDto {
  id: string;
  word: string;
  targetForm: string;
  sentenceWithBlank: string;
  completedSentence: string;
  sentenceVn: string;
  grammarContextTip: string;
  explanation: string;
  source?: "DATABASE" | "AI_GENERATED" | "DICTIONARY_FALLBACK";
  partOfSpeech?: string;
  cefrLevel?: string;
  definitionEn?: string;
  definitionVn?: string;
  topic?: string;
  audioUrl?: string;
  acceptableVariants?: string[];
}

interface ApiResponse<T> {
  code: number;
  message?: string;
  result: T;
}

// ── Fallback Sample Data ────────────────────────────────────────────────────────
export const FALLBACK_LEARN_WORDS: LearnWordItemDto[] = [
  {
    id: "fb-compromise",
    word: "compromise",
    phonetic: "/ˈkɒmprəmaɪz/",
    phoneticUs: "/ˈkɑːmprəmaɪz/",
    phoneticUk: "/ˈkɒmprəmaɪz/",
    partOfSpeech: "noun / verb",
    cefrLevel: "B2",
    topic: "Business",
    definitionEn: "An agreement in an argument, arrived at by people reducing their demands or changing their opinion in order to agree.",
    definitionVn: "Sự thoả hiệp, sự nhượng bộ; dàn xếp đôi bên cùng có lợi",
    exampleEn: "Both parties agreed to make a compromise to finalize the joint venture agreement.",
    exampleVn: "Cả hai bên đã đồng ý đưa ra sự thỏa hiệp để hoàn tất hợp đồng liên doanh.",
    synonyms: ["agreement", "concession", "settlement"],
    antonyms: ["dispute", "intransigence"],
    collocations: ["reach a compromise", "make a compromise", "compromise on price"],
  },
  {
    id: "fb-negotiate",
    word: "negotiate",
    phonetic: "/nəˈɡoʊ.ʃi.eɪt/",
    phoneticUs: "/nəˈɡoʊ.ʃi.eɪt/",
    phoneticUk: "/nəˈɡəʊ.ʃi.eɪt/",
    partOfSpeech: "verb",
    cefrLevel: "B2",
    topic: "Business",
    definitionEn: "To have formal discussions with someone in order to reach an agreement.",
    definitionVn: "Đàm phán, thương lượng các điều khoản hợp đồng",
    exampleEn: "The government will negotiate a new contract with the international supplier.",
    exampleVn: "Chính phủ sẽ đàm phán một hợp đồng mới với nhà cung cấp quốc tế.",
    synonyms: ["bargain", "confer", "discuss terms"],
    antonyms: ["dictate"],
    collocations: ["negotiate a deal", "negotiate terms", "successfully negotiate"],
  },
  {
    id: "fb-mitigate",
    word: "mitigate",
    phonetic: "/ˈmɪt.ɪ.ɡeɪt/",
    phoneticUs: "/ˈmɪt̬.ə.ɡeɪt/",
    phoneticUk: "/ˈmɪt.ɪ.ɡeɪt/",
    partOfSpeech: "verb",
    cefrLevel: "C1",
    topic: "Environment",
    definitionEn: "To make something less severe, harmful, or painful.",
    definitionVn: "Làm giảm nhẹ, làm dịu bớt mức độ nghiêm trọng hay tác hại",
    exampleEn: "Governments must implement long-term policies to mitigate the adverse impact of climate change.",
    exampleVn: "Các chính phủ cần thực hiện các chính sách dài hạn để giảm thiểu tác động bất lợi của biến đổi khí hậu.",
    synonyms: ["alleviate", "lessen", "attenuate"],
    antonyms: ["exacerbate", "aggravate"],
    collocations: ["mitigate risk", "mitigate the effects", "mitigate damages"],
  },
  {
    id: "fb-deadline",
    word: "deadline",
    phonetic: "/ˈded.laɪn/",
    phoneticUs: "/ˈded.laɪn/",
    phoneticUk: "/ˈded.laɪn/",
    partOfSpeech: "noun",
    cefrLevel: "B1",
    topic: "Business",
    definitionEn: "A specific time or day by which something must be finished.",
    definitionVn: "Hạn chót, thời hạn cuối cùng phải hoàn thành công việc",
    exampleEn: "The engineering team worked overtime to meet the strict project deadline.",
    exampleVn: "Đội ngũ kỹ thuật đã làm thêm giờ để kịp hạn chót nghiêm ngặt của dự án.",
    synonyms: ["due date", "cutoff", "time limit"],
    antonyms: [],
    collocations: ["meet a deadline", "miss a deadline", "tight deadline"],
  },
  {
    id: "fb-destination",
    word: "destination",
    phonetic: "/ˌdes.təˈneɪ.ʃən/",
    phoneticUs: "/ˌdes.təˈneɪ.ʃən/",
    phoneticUk: "/ˌdes.tɪˈneɪ.ʃən/",
    partOfSpeech: "noun",
    cefrLevel: "B1",
    topic: "Travel & Adventures",
    definitionEn: "The place where someone is going or where something is being sent or taken.",
    definitionVn: "Điểm đến, nơi đích của một chuyến hành trình",
    exampleEn: "We arrived at our final vacation destination after a ten-hour overseas flight.",
    exampleVn: "Chúng tôi đã đến điểm đến nghỉ dưỡng cuối cùng sau chuyến bay mười tiếng.",
    synonyms: ["stop", "journey's end", "terminal"],
    antonyms: ["departure point"],
    collocations: ["tourist destination", "final destination", "reach your destination"],
  },
  {
    id: "fb-itinerary",
    word: "itinerary",
    phonetic: "/aɪˈtɪn.ə.rer.i/",
    phoneticUs: "/aɪˈtɪn.ə.rer.i/",
    phoneticUk: "/aɪˈtɪn.ər.ər.i/",
    partOfSpeech: "noun",
    cefrLevel: "B1",
    topic: "Travel & Adventures",
    definitionEn: "A detailed plan or schedule of a journey, including the route and places to visit.",
    definitionVn: "Lịch trình chi tiết của một chuyến đi hay hành trình",
    exampleEn: "The tour guide provided travelers with an organized daily itinerary.",
    exampleVn: "Hướng dẫn viên du lịch đã cung cấp cho du khách một lịch trình hàng ngày có tổ chức.",
    synonyms: ["schedule", "route", "travel plan"],
    antonyms: [],
    collocations: ["travel itinerary", "detailed itinerary", "planned itinerary"],
  },
];

export const FALLBACK_MULTIPLE_CHOICE: MultipleChoiceItemDto[] = [
  {
    id: "mc-compromise",
    question: "What is the primary meaning of 'compromise'?",
    targetWord: "compromise",
    options: [
      "To reach an agreement by mutual concession",
      "To refuse any discussion or change",
      "To demand full payment immediately",
      "To terminate a signed agreement",
    ],
    correctAnswer: "To reach an agreement by mutual concession",
    correctIndex: 0,
    phonetic: "/ˈkɒmprəmaɪz/",
    partOfSpeech: "noun / verb",
    sentence: "Both parties agreed to make a compromise to finalize the joint venture agreement.",
    explanation: "'Compromise' means finding a middle ground where both parties accept some terms and give up others.",
  },
  {
    id: "mc-mitigate",
    question: "Select the sentence that uses 'mitigate' correctly:",
    targetWord: "mitigate",
    options: [
      "New drainage infrastructure will mitigate urban flooding risks.",
      "The heavy storm will mitigate the trees in the forest.",
      "She asked the barista to mitigate extra sugar into the coffee.",
      "He decided to mitigate his car to work every day.",
    ],
    correctAnswer: "New drainage infrastructure will mitigate urban flooding risks.",
    correctIndex: 0,
    phonetic: "/ˈmɪt.ɪ.ɡeɪt/",
    partOfSpeech: "verb",
    sentence: "Governments must implement long-term policies to mitigate the adverse impact of climate change.",
    explanation: "'Mitigate' means to make something less severe or harmful.",
  },
];

export const FALLBACK_TYPE_WORD: TypeWordItemDto[] = [
  {
    id: "tw-compromise",
    targetWord: "compromise",
    promptDefinition: "An agreement where both parties accept less than they originally asked for (sự thoả hiệp).",
    definitionEn: "An agreement where both parties accept less than they originally asked for.",
    definitionVn: "Sự thoả hiệp, sự nhượng bộ",
    firstLetter: "c",
    characterCount: 10,
    partOfSpeech: "noun / verb",
    phonetic: "/ˈkɒmprəmaɪz/",
    exampleWithMask: "Both parties agreed to make a ________ to finalize the contract.",
    exampleSentence: "Both parties agreed to make a compromise to finalize the contract.",
  },
  {
    id: "tw-negotiate",
    targetWord: "negotiate",
    promptDefinition: "To have formal discussions with someone in order to reach an agreement (thương lượng, đàm phán).",
    definitionEn: "To have formal discussions with someone in order to reach an agreement.",
    definitionVn: "Đàm phán, thương lượng",
    firstLetter: "n",
    characterCount: 9,
    partOfSpeech: "verb",
    phonetic: "/nəˈɡoʊ.ʃi.eɪt/",
    exampleWithMask: "The committee will ________ a new contract with the supplier.",
    exampleSentence: "The committee will negotiate a new contract with the supplier.",
  },
];

export const FALLBACK_FILL_BLANK: FillBlankItemDto[] = [
  {
    id: "fb-item-mitigate",
    word: "mitigate",
    targetForm: "mitigate",
    topic: "Environment",
    partOfSpeech: "verb",
    cefrLevel: "C1",
    definitionEn: "To make something less severe, harmful, or painful.",
    definitionVn: "Làm giảm nhẹ, làm dịu bớt mức độ nghiêm trọng hay tác hại",
    sentenceWithBlank: "Governments must implement proactive policies to ______ the adverse impacts of rising sea levels.",
    completedSentence: "Governments must implement proactive policies to mitigate the adverse impacts of rising sea levels.",
    sentenceVn: "Các chính phủ phải thực hiện các chính sách chủ động để giảm thiểu những tác động bất lợi của mực nước biển dâng cao.",
    grammarContextTip: "Follows infinitive marker 'to' -> requires base form verb (infinitive).",
    explanation: "'mitigate' is a formal transitive verb meaning to alleviate or reduce the severity of something.",
    source: "DATABASE",
    acceptableVariants: ["mitigate", "mitigating"],
  },
  {
    id: "fb-item-issues",
    word: "issue",
    targetForm: "issues",
    topic: "Business",
    partOfSpeech: "noun (plural)",
    cefrLevel: "B1",
    definitionEn: "An important topic or problem for debate or discussion.",
    definitionVn: "Vấn đề, khía cạnh quan trọng cần thảo luận hoặc giải quyết",
    sentenceWithBlank: "The executive board addressed several financial ______ this quarter.",
    completedSentence: "The executive board addressed several financial issues this quarter.",
    sentenceVn: "Hội đồng quản trị đã giải quyết một số vấn đề tài chính trong quý này.",
    grammarContextTip: "Requires a plural countable noun (-s) after the quantifier 'several financial'.",
    explanation: "Here, 'issues' refers to complex problems or matters for debate. 'Several' requires plural count inflection.",
    source: "DATABASE",
    acceptableVariants: ["issues", "issue"],
  },
  {
    id: "fb-item-negotiate",
    word: "negotiate",
    targetForm: "negotiate",
    topic: "Business",
    partOfSpeech: "verb (infinitive)",
    cefrLevel: "B2",
    definitionEn: "To have formal discussions with someone in order to reach an agreement.",
    definitionVn: "Đàm phán, thương lượng các điều khoản hợp đồng",
    sentenceWithBlank: "The procurement committee agreed to ______ better contract terms with the supplier.",
    completedSentence: "The procurement committee agreed to negotiate better contract terms with the supplier.",
    sentenceVn: "Ủy ban mua sắm đã đồng ý đàm phán các điều khoản hợp đồng tốt hơn với nhà cung cấp.",
    grammarContextTip: "Infinitive verb needed following the verb pattern 'agreed to + [base verb]'.",
    explanation: "Following 'agreed to', the bare verb 'negotiate' forms the infinitive 'to negotiate'.",
    source: "DATABASE",
    acceptableVariants: ["negotiate", "negotiating"],
  },
  {
    id: "fb-item-deadline",
    word: "deadline",
    targetForm: "deadline",
    topic: "Business",
    partOfSpeech: "noun (singular)",
    cefrLevel: "B1",
    definitionEn: "A specific time or day by which something must be finished.",
    definitionVn: "Hạn chót, thời hạn cuối cùng phải hoàn thành công việc",
    sentenceWithBlank: "All project milestones must be submitted before Friday's strict ______.",
    completedSentence: "All project milestones must be submitted before Friday's strict deadline.",
    sentenceVn: "Tất cả các mốc của dự án phải được nộp trước hạn chót nghiêm ngặt vào thứ Sáu.",
    grammarContextTip: "Singular noun specifying a cutoff time following possessive modifier 'Friday's strict'.",
    explanation: "A 'deadline' is the final date or hour by which tasks must be completed.",
    source: "DATABASE",
    acceptableVariants: ["deadline", "deadlines"],
  },
  {
    id: "fb-item-destinations",
    word: "destination",
    targetForm: "destinations",
    topic: "Travel & Adventures",
    partOfSpeech: "noun (plural)",
    cefrLevel: "B1",
    definitionEn: "The place where someone is going or where something is being sent or taken.",
    definitionVn: "Điểm đến, nơi đích của một chuyến hành trình",
    sentenceWithBlank: "Kyoto and Da Nang are among the most popular holiday ______ for international travelers.",
    completedSentence: "Kyoto and Da Nang are among the most popular holiday destinations for international travelers.",
    sentenceVn: "Kyoto và Đà Nẵng nằm trong số những điểm đến nghỉ dưỡng phổ biến nhất cho du khách quốc tế.",
    grammarContextTip: "Plural countable noun (-s) needed following 'among the most popular holiday...'",
    explanation: "Multiple geographic locations ('Kyoto and Da Nang') necessitate the plural noun form 'destinations'.",
    source: "DATABASE",
    acceptableVariants: ["destinations", "destination"],
  },
  {
    id: "fb-item-itinerary",
    word: "itinerary",
    targetForm: "itinerary",
    topic: "Travel & Adventures",
    partOfSpeech: "noun",
    cefrLevel: "B1",
    definitionEn: "A detailed plan or schedule of a journey, including the route and places to visit.",
    definitionVn: "Lịch trình chi tiết của một chuyến đi hay hành trình",
    sentenceWithBlank: "Please review the detailed flight and hotel ______ prior to departure.",
    completedSentence: "Please review the detailed flight and hotel itinerary prior to departure.",
    sentenceVn: "Vui lòng xem lại hành trình chi tiết chuyến bay và khách sạn trước khi khởi hành.",
    grammarContextTip: "Singular noun referring to the comprehensive travel route and timetable.",
    explanation: "An 'itinerary' is a planned route or schedule of a journey.",
    source: "DATABASE",
    acceptableVariants: ["itinerary"],
  },
];

// ── Practiced Word Tracking & Persistence ──────────────────────────────────────

export interface PracticedWordRecord {
  word: string;
  isCorrect?: boolean;
  rating?: string;
  status?: "learning" | "mastered";
}

function getPracticedWordsKey(topic: string) {
  return `parrotalk_practiced_words_${topic.trim().toLowerCase().replace(/\s+/g, "_")}`;
}

function getWordStatusKey(topic: string) {
  return `parrotalk_word_statuses_${topic.trim().toLowerCase().replace(/\s+/g, "_")}`;
}

export function getPracticedWordsForTopic(topic: string): string[] {
  if (!topic || typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(getPracticedWordsKey(topic));
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed)
      ? parsed.filter((w): w is string => typeof w === "string")
      : [];
  } catch {
    return [];
  }
}

export function getLocalWordStatusesForTopic(
  topic: string
): Record<string, "learning" | "mastered"> {
  if (!topic || typeof window === "undefined") return {};
  try {
    const raw = localStorage.getItem(getWordStatusKey(topic));
    if (!raw) return {};
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

export async function recordPracticedWords(
  topic: string,
  records: PracticedWordRecord[]
): Promise<void> {
  if (!topic || !records || records.length === 0 || typeof window === "undefined") {
    return;
  }

  try {
    const existingWords = getPracticedWordsForTopic(topic);
    const existingSet = new Set(existingWords.map((w) => w.toLowerCase()));
    const statuses = getLocalWordStatusesForTopic(topic);

    for (const rec of records) {
      const w = rec.word?.trim().toLowerCase();
      if (!w) continue;
      if (!existingSet.has(w)) {
        existingSet.add(w);
        existingWords.push(w);
      }
      if (rec.status === "mastered" || rec.rating === "easy") {
        statuses[w] = "mastered";
      } else if (statuses[w] !== "mastered") {
        statuses[w] = "learning";
      }
    }

    localStorage.setItem(
      getPracticedWordsKey(topic),
      JSON.stringify(existingWords.slice(-250))
    );
    localStorage.setItem(getWordStatusKey(topic), JSON.stringify(statuses));
  } catch (err) {
    console.warn("[vocabularyPracticeService] Failed to save practiced words locally:", err);
  }

  try {
    await axiosInstance.post("/vocabulary/practice/submit", records, {
      timeout: 8000,
    });
  } catch {
    // Ignore network or unauthenticated errors silently
  }
}

function buildPracticeParams(topic: string, limit: number) {
  const excluded = getPracticedWordsForTopic(topic);
  const exclude = excluded.length > 0 ? excluded.slice(-150).join(",") : undefined;
  return exclude ? { topic, limit, exclude } : { topic, limit };
}

// ── API Methods ─────────────────────────────────────────────────────────────────

/**
 * Fetch flashcards for Learn Words practice mode.
 * Endpoint: GET /api/vocabulary/practice/learn-words?topic=${topic}&limit=${limit}
 */
export async function fetchLearnWords(
  topic: string,
  limit: number = 10
): Promise<LearnWordItemDto[]> {
  try {
    const response = await axiosInstance.get<ApiResponse<LearnWordItemDto[]>>(
      "/vocabulary/practice/learn-words",
      {
        params: buildPracticeParams(topic, limit),
        timeout: 15000,
      }
    );

    if (response.data?.result && Array.isArray(response.data.result) && response.data.result.length > 0) {
      return response.data.result;
    }
  } catch (error) {
    console.warn(`[vocabularyPracticeService] fetchLearnWords API offline or failed for topic '${topic}', using fallback:`, error);
  }

  // Fallback filtering by topic if possible
  const topicLower = topic.toLowerCase();
  const matched = FALLBACK_LEARN_WORDS.filter(
    (item) => item.topic && (item.topic.toLowerCase().includes(topicLower) || topicLower.includes(item.topic.toLowerCase()))
  );

  return matched.length > 0 ? matched : FALLBACK_LEARN_WORDS.slice(0, limit);
}

/**
 * Fetch Multiple Choice quiz questions for the topic.
 * Endpoint: GET /api/vocabulary/practice/multiple-choice?topic=${topic}&limit=${limit}
 */
export async function fetchMultipleChoice(
  topic: string,
  limit: number = 10
): Promise<MultipleChoiceItemDto[]> {
  try {
    const response = await axiosInstance.get<ApiResponse<MultipleChoiceItemDto[]>>(
      "/vocabulary/practice/multiple-choice",
      {
        params: buildPracticeParams(topic, limit),
        timeout: 15000,
      }
    );

    if (response.data?.result && Array.isArray(response.data.result) && response.data.result.length > 0) {
      return response.data.result;
    }
  } catch (error) {
    console.warn(`[vocabularyPracticeService] fetchMultipleChoice API offline for '${topic}', using fallback:`, error);
  }

  return FALLBACK_MULTIPLE_CHOICE.slice(0, limit);
}

/**
 * Fetch Type the Word recall questions for the topic.
 * Endpoint: GET /api/vocabulary/practice/type-word?topic=${topic}&limit=${limit}
 */
export async function fetchTypeWord(
  topic: string,
  limit: number = 10
): Promise<TypeWordItemDto[]> {
  try {
    const response = await axiosInstance.get<ApiResponse<TypeWordItemDto[]>>(
      "/vocabulary/practice/type-word",
      {
        params: buildPracticeParams(topic, limit),
        timeout: 15000,
      }
    );

    if (response.data?.result && Array.isArray(response.data.result) && response.data.result.length > 0) {
      return response.data.result;
    }
  } catch (error) {
    console.warn(`[vocabularyPracticeService] fetchTypeWord API offline for '${topic}', using fallback:`, error);
  }

  return FALLBACK_TYPE_WORD.slice(0, limit);
}

/**
 * Fetch Fill in the Blank questions powered by the Hybrid AI sentence service.
 * Endpoint: GET /api/vocabulary/practice/fill-blank?topic=${topic}&limit=${limit}
 */
export async function fetchFillBlank(
  topic: string,
  limit: number = 10
): Promise<FillBlankItemDto[]> {
  try {
    const response = await axiosInstance.get<ApiResponse<FillBlankItemDto[]>>(
      "/vocabulary/practice/fill-blank",
      {
        params: buildPracticeParams(topic, limit),
        timeout: 15000,
      }
    );

    if (response.data?.result && Array.isArray(response.data.result) && response.data.result.length > 0) {
      return response.data.result;
    }
  } catch (error) {
    console.warn(`[vocabularyPracticeService] fetchFillBlank API offline for '${topic}', using fallback:`, error);
  }

  const topicLower = topic.toLowerCase();
  const matched = FALLBACK_FILL_BLANK.filter(
    (item) =>
      item.topic &&
      (item.topic.toLowerCase().includes(topicLower) ||
        topicLower.includes(item.topic.toLowerCase()))
  );

  return (matched.length > 0 ? matched : FALLBACK_FILL_BLANK).slice(0, limit);
}
