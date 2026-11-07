import type {
  RichVocabularyDetailData,
  WordSenseItem,
  WordPhrasalVerbItem,
  WordIdiomItem,
} from "./types";
import type { VocabularyDetailDto } from "../../types";

export const SAMPLE_ISSUE_DETAIL: RichVocabularyDetailData = {
  id: "vocab-issue",
  word: "issue",
  isNew: true,
  partOfSpeechSummary: "noun / verb · B1–C1 across senses",
  vietnameseSummary: "vấn đề; số báo; cấp, phát hành",
  pronunciations: {
    uk: { ipa: "/ˈɪʃuː/" },
    us: { ipa: "/ˈɪʃuː/" },
  },
  topics: ["Business", "Media", "Administration", "Finance"],
  senses: [
    {
      id: "sense-1",
      orderNumber: "01",
      title: "A problem or matter",
      partOfSpeech: "noun",
      countability: "countable",
      cefrLevel: "B1",
      topicTag: "Business",
      definitionEn: "A problem or matter that needs attention or discussion.",
      definitionVn: "vấn đề cần được giải quyết hoặc thảo luận",
      usageContextNote: "At work · discussing concerns and finding solutions",
      inContext: {
        english: "We need to resolve this issue before the next meeting.",
        vietnamese: "Chúng ta cần giải quyết vấn đề này trước cuộc họp tiếp theo.",
      },
      collocations: ["resolve an issue", "raise an issue", "address an issue", "core issue"],
      synonyms: ["problem", "matter", "concern", "challenge"],
      additionalExamples: [
        {
          english: "She raised an important issue during the meeting.",
          vietnamese: "Cô ấy nêu một vấn đề quan trọng trong cuộc họp.",
        },
      ],
    },
    {
      id: "sense-2",
      orderNumber: "02",
      title: "An edition of a publication",
      partOfSpeech: "noun",
      countability: "countable",
      cefrLevel: "B1",
      topicTag: "Media",
      definitionEn: "One edition of a newspaper or magazine.",
      definitionVn: "số báo; số tạp chí",
      usageContextNote: "Publishing · a particular edition, not a problem",
      inContext: {
        english: "The latest issue includes an interview with the founder.",
        vietnamese: "Số mới nhất có bài phỏng vấn với người sáng lập.",
      },
      collocations: ["latest issue", "current issue", "back issue", "special issue"],
      synonyms: ["edition", "number", "copy", "release"],
    },
    {
      id: "sense-3",
      orderNumber: "03",
      title: "Officially provide or release",
      partOfSpeech: "verb",
      countability: "transitive",
      cefrLevel: "B2",
      topicTag: "Administration",
      definitionEn: "To officially provide or distribute something.",
      definitionVn: "cấp; phát hành; ban hành",
      usageContextNote: "Official use · documents, permits, and public statements",
      inContext: {
        english: "The company will issue new ID cards on Monday.",
        vietnamese: "Công ty sẽ cấp thẻ nhân viên mới vào thứ Hai.",
      },
      collocations: ["issue a statement", "issue a permit", "issue a passport", "issue a warning"],
      synonyms: ["distribute", "supply", "furnish", "release"],
    },
    {
      id: "sense-4",
      orderNumber: "04",
      title: "Make shares available",
      partOfSpeech: "verb",
      countability: "transitive",
      cefrLevel: "C1",
      topicTag: "Finance",
      definitionEn: "To offer new shares or other securities for people to buy.",
      definitionVn: "phát hành cổ phiếu hoặc chứng khoán",
      usageContextNote: "Finance · creating and offering securities, not trading existing shares",
      inContext: {
        english: "The company issued new shares to raise capital.",
        vietnamese: "Công ty đã phát hành cổ phiếu mới để huy động vốn.",
      },
      collocations: ["issue shares", "issue bonds", "share issue", "rights issue"],
      synonyms: ["float", "offer", "underwrite"],
    },
  ],
  phrasalVerbs: [
    {
      phrasalVerb: "issue from",
      register: "formal / literary",
      definitionEn: "To come out of a place, or to originate from something.",
      definitionVn: "thoát ra từ; bắt nguồn từ",
      exampleEn: "Smoke issued from the chimney.",
      exampleVn: "Khói thoát ra từ ống khói.",
    },
  ],
  idioms: [
    {
      idiom: "take issue with",
      relatedSenseNote: "Related to meaning 01 · a problem or matter",
      definitionEn: "To disagree with or object to someone or something.",
      definitionVn: "không đồng tình với; phản đối",
      exampleEn: "I take issue with the claim that the project was a failure.",
      exampleVn: "Tôi không đồng tình với nhận định rằng dự án đã thất bại.",
    },
    {
      idiom: "at issue",
      relatedSenseNote: "Related to meaning 01 · a problem or matter",
      definitionEn: "Being discussed or disputed: the matter under consideration.",
      definitionVn: "đang được thảo luận hoặc tranh luận; vấn đề đang được xem xét",
      exampleEn: "The main point at issue is who should pay for the repairs.",
      exampleVn: "Điểm chính đang được tranh luận là ai phải trả chi phí sửa chữa.",
    },
  ],
};

function tryParseJson(value: any): any {
  if (typeof value !== "string") return value;
  const trimmed = value.trim();
  if ((trimmed.startsWith("[") && trimmed.endsWith("]")) || (trimmed.startsWith("{") && trimmed.endsWith("}"))) {
    try {
      return JSON.parse(trimmed);
    } catch {
      return value;
    }
  }
  return value;
}

function cleanStringList(list?: any[], fallbackKey?: string): string[] {
  if (!list || list.length === 0) return [];
  const result: string[] = [];

  for (const item of list) {
    const parsed = tryParseJson(item);
    if (Array.isArray(parsed)) {
      for (const sub of parsed) {
        if (typeof sub === "string" && sub.trim()) {
          result.push(sub.trim());
        } else if (sub && typeof sub === "object") {
          const val = fallbackKey && sub[fallbackKey] ? sub[fallbackKey] : Object.values(sub)[0];
          if (val && typeof val === "string") result.push(val.trim());
        }
      }
    } else if (typeof parsed === "string" && parsed.trim()) {
      result.push(parsed.trim());
    } else if (parsed && typeof parsed === "object") {
      const val = fallbackKey && parsed[fallbackKey] ? parsed[fallbackKey] : Object.values(parsed)[0];
      if (val && typeof val === "string") result.push(val.trim());
    }
  }

  return result.filter(Boolean);
}

export function transformDtoToRichDetail(dto: VocabularyDetailDto): RichVocabularyDetailData {
  if (dto.word?.toLowerCase() === "issue" && (!dto.senses || dto.senses.length === 0)) {
    return SAMPLE_ISSUE_DETAIL;
  }

  const rawCleanSynonyms = cleanStringList(dto.synonyms, "synonym");
  const rawCleanCollocations = cleanStringList(dto.collocations, "collocation");

  // 1. Process Senses: Check if backend provided structured senses
  const senses: WordSenseItem[] = [];

  if (dto.senses && dto.senses.length > 0) {
    dto.senses.forEach((s, idx) => {
      senses.push({
        id: s.id || `sense-${idx + 1}`,
        orderNumber: s.orderNumber || String(idx + 1).padStart(2, "0"),
        title: s.title || (s.definitionEn ? s.definitionEn.split(".")[0].trim() : `Sense ${idx + 1}`),
        partOfSpeech: s.partOfSpeech || dto.partOfSpeech || "noun",
        countability: s.countability,
        cefrLevel: s.cefrLevel || dto.cefrLevel || "B1",
        topicTag: s.topicTag || dto.topic || "General",
        definitionEn: s.definitionEn,
        definitionVn: s.definitionVn || dto.commonMeaningVi || "",
        usageContextNote: s.usageContextNote,
        inContext: {
          english: s.exampleEn || (dto.examples && dto.examples[idx]) || `An example with "${dto.word}".`,
          vietnamese: s.exampleVn || "",
        },
        collocations: s.collocations && s.collocations.length > 0 ? s.collocations : rawCleanCollocations,
        synonyms: s.synonyms && s.synonyms.length > 0 ? s.synonyms : rawCleanSynonyms,
      });
    });
  } else if (dto.definitions && dto.definitions.length > 0) {
    // Legacy / Unstructured: check if definitions contains raw JSON string
    dto.definitions.forEach((defItem) => {
      const parsed = tryParseJson(defItem);

      if (Array.isArray(parsed)) {
        // Parsed as an array of sense objects: [{pos, definition, definition_vn, example, example_vn}]
        parsed.forEach((item) => {
          if (item && typeof item === "object") {
            const defEn = (item.definition || item.meaning || "").trim();
            const defVn = (item.definition_vn || item.meaning_vn || dto.commonMeaningVi || "").trim();
            const pos = item.pos || dto.partOfSpeech || "noun";
            const exEn = (item.example || item.sentence || "").trim();
            const exVn = (item.example_vn || item.sentence_vn || "").trim();

            let title = defEn;
            if (title.includes(".")) {
              title = title.split(".")[0].trim();
            }
            if (title.length > 60) {
              title = title.substring(0, 57) + "...";
            }
            if (!title) title = defVn || dto.word;

            const orderIdx = senses.length + 1;
            senses.push({
              id: `sense-${orderIdx}`,
              orderNumber: String(orderIdx).padStart(2, "0"),
              title,
              partOfSpeech: pos,
              countability: pos.toLowerCase() === "noun" ? "countable" : undefined,
              cefrLevel: dto.cefrLevel || "B1",
              topicTag: dto.topic || "General",
              definitionEn: defEn || dto.word,
              definitionVn: defVn,
              inContext: {
                english: exEn || `An example with "${dto.word}".`,
                vietnamese: exVn,
              },
              collocations: rawCleanCollocations,
              synonyms: rawCleanSynonyms,
            });
          } else if (typeof item === "string" && item.trim()) {
            const orderIdx = senses.length + 1;
            senses.push({
              id: `sense-${orderIdx}`,
              orderNumber: String(orderIdx).padStart(2, "0"),
              title: item.split(".")[0].trim() || item,
              partOfSpeech: dto.partOfSpeech || "noun",
              cefrLevel: dto.cefrLevel || "B1",
              topicTag: dto.topic || "General",
              definitionEn: item,
              definitionVn: dto.commonMeaningVi || "",
              inContext: {
                english: (dto.examples && dto.examples[orderIdx - 1]) || `An example with "${dto.word}".`,
                vietnamese: "",
              },
              collocations: rawCleanCollocations,
              synonyms: rawCleanSynonyms,
            });
          }
        });
      } else if (typeof defItem === "string" && defItem.trim()) {
        const orderIdx = senses.length + 1;
        senses.push({
          id: `sense-${orderIdx}`,
          orderNumber: String(orderIdx).padStart(2, "0"),
          title: defItem.split(".")[0].trim() || defItem,
          partOfSpeech: dto.partOfSpeech || "noun",
          cefrLevel: dto.cefrLevel || "B1",
          topicTag: dto.topic || "General",
          definitionEn: defItem,
          definitionVn: dto.commonMeaningVi || "",
          inContext: {
            english: (dto.examples && dto.examples[orderIdx - 1]) || `An example with "${dto.word}".`,
            vietnamese: "",
          },
          collocations: rawCleanCollocations,
          synonyms: rawCleanSynonyms,
        });
      }
    });
  }

  // 2. Process Phrasal Verbs
  const phrasalVerbs: WordPhrasalVerbItem[] = [];
  if (dto.structuredPhrasalVerbs && dto.structuredPhrasalVerbs.length > 0) {
    dto.structuredPhrasalVerbs.forEach((pv) => {
      phrasalVerbs.push({
        phrasalVerb: pv.phrasalVerb,
        register: pv.register,
        definitionEn: pv.definitionEn,
        definitionVn: pv.definitionVn || "",
        exampleEn: pv.exampleEn || "",
        exampleVn: pv.exampleVn || "",
      });
    });
  } else if (dto.phrasalVerbs && dto.phrasalVerbs.length > 0) {
    dto.phrasalVerbs.forEach((item) => {
      const parsed = tryParseJson(item);
      const list = Array.isArray(parsed) ? parsed : [parsed];
      list.forEach((sub) => {
        if (sub && typeof sub === "object") {
          const phrase = sub.phrase || sub.phrasal_verb || sub.word || "";
          if (phrase) {
            phrasalVerbs.push({
              phrasalVerb: phrase,
              register: sub.register || "common",
              definitionEn: sub.definition || "",
              definitionVn: sub.definition_vn || "",
              exampleEn: sub.example || "",
              exampleVn: sub.example_vn || "",
            });
          }
        } else if (typeof sub === "string" && sub.trim()) {
          phrasalVerbs.push({
            phrasalVerb: sub.trim(),
            definitionEn: `Phrasal verb formed with "${dto.word}".`,
            definitionVn: "",
            exampleEn: `Example sentence with ${sub}.`,
            exampleVn: "",
          });
        }
      });
    });
  }

  // 3. Process Idioms
  const idioms: WordIdiomItem[] = [];
  if (dto.structuredIdioms && dto.structuredIdioms.length > 0) {
    dto.structuredIdioms.forEach((idm) => {
      idioms.push({
        idiom: idm.idiom,
        relatedSenseNote: idm.relatedSenseNote,
        definitionEn: idm.definitionEn,
        definitionVn: idm.definitionVn || "",
        exampleEn: idm.exampleEn || "",
        exampleVn: idm.exampleVn || "",
      });
    });
  } else if (dto.idioms && dto.idioms.length > 0) {
    dto.idioms.forEach((item) => {
      const parsed = tryParseJson(item);
      const list = Array.isArray(parsed) ? parsed : [parsed];
      list.forEach((sub) => {
        if (sub && typeof sub === "object") {
          const phrase = sub.phrase || sub.idiom || "";
          if (phrase) {
            idioms.push({
              idiom: phrase,
              relatedSenseNote: sub.related_note || `Related to "${dto.word}"`,
              definitionEn: sub.definition || "",
              definitionVn: sub.definition_vn || "",
              exampleEn: sub.example || "",
              exampleVn: sub.example_vn || "",
            });
          }
        } else if (typeof sub === "string" && sub.trim()) {
          idioms.push({
            idiom: sub.trim(),
            relatedSenseNote: `Expression with "${dto.word}"`,
            definitionEn: `Idiomatic expression using "${dto.word}".`,
            definitionVn: "",
            exampleEn: `Example with ${sub}.`,
            exampleVn: "",
          });
        }
      });
    });
  }

  // Topics list
  const topics = dto.topic
    ? [dto.topic]
    : senses.map((s) => s.topicTag).filter(Boolean);
  const uniqueTopics = Array.from(new Set(topics.length > 0 ? topics : ["General"]));

  return {
    id: dto.id || `vocab-${dto.word}`,
    word: dto.word,
    isNew: true,
    partOfSpeechSummary: `${dto.partOfSpeech || 'noun'} · ${dto.cefrLevel || 'B1'} across senses`,
    vietnameseSummary: dto.commonMeaningVi || (senses[0]?.definitionVn) || "",
    pronunciations: {
      uk: { ipa: dto.phonetic || "/.../", audioUrl: dto.audioUkUrl },
      us: { ipa: dto.phonetic || "/.../", audioUrl: dto.audioUsUrl },
    },
    topics: uniqueTopics,
    senses: senses.length > 0 ? senses : SAMPLE_ISSUE_DETAIL.senses,
    phrasalVerbs,
    idioms,
  };
}
