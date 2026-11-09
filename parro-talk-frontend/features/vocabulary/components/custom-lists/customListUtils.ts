export interface MinimalSenseForSelection {
  id: string;
  cefrLevel?: string;
  partOfSpeech?: string;
  definitionEn: string;
}

const DEFAULT_CEFR_SET = new Set(["A1", "A2", "B1", "B2"]);

export function getDefaultSelectedSenseIds(
  senses: MinimalSenseForSelection[]
): string[] {
  if (!senses || senses.length === 0) return [];
  const common = senses
    .filter(
      (s) => !s.cefrLevel || DEFAULT_CEFR_SET.has(s.cefrLevel.toUpperCase())
    )
    .map((s) => s.id);
  if (common.length > 0) {
    return common;
  }
  return [senses[0].id];
}

export function formatQuestionCategoryLabel(category?: string): string {
  switch (category) {
    case "CONTEXTUAL_CLOZE":
      return "Điền từ theo ngữ cảnh";
    case "SYNONYM_IN_CONTEXT":
      return "Từ đồng nghĩa";
    case "ANTONYM_IN_CONTEXT":
      return "Từ trái nghĩa";
    case "DEFINITION_MATCH":
      return "Định nghĩa";
    case "COLLOCATION_PREPOSITION":
      return "Collocation & Giới từ";
    case "WORD_FORM":
      return "Word Family / Từ loại";
    case "IDIOM_PHRASAL_VERB":
      return "Idiom & Phrasal Verb";
    case "USAGE_ERROR_DETECTION":
      return "Ứng dụng câu chuẩn";
    default:
      return "Multiple Choice";
  }
}

export function formatGenerationSourceBadge(source?: string): string {
  switch (source) {
    case "USER_LIST_FINALIZE_AI":
      return "Chốt List AI";
    case "USER_LIST_MORE_AI":
      return "Tạo thêm AI";
    case "PIPELINE_BATCH":
      return "Kho câu hỏi chung";
    case "ADMIN_MANUAL":
      return "Biên tập viên";
    default:
      return "AI Question";
  }
}

export function formatDataSourceBadge(source?: string): string {
  switch (source) {
    case "OXFORD_MW_HYBRID":
      return "Oxford + Merriam-Webster";
    case "OXFORD_CRAWLER":
      return "Oxford Dictionary";
    case "MERRIAM_WEBSTER_API":
      return "Merriam-Webster";
    case "IMPORT_JSON":
      return "Kho từ vựng sẵn có";
    case "AI_GENERATED":
      return "AI Enriched";
    default:
      return "Từ điển";
  }
}

export function resolveIsOwner(
  detail?: { isOwner?: boolean; owner?: boolean } | null
): boolean {
  if (!detail) return false;
  if (typeof detail.isOwner === "boolean") {
    return detail.isOwner;
  }
  return Boolean(detail.owner);
}

export function resolveItemDisplayWord(
  item?: {
    word?: string;
    normalizedWord?: string;
    rawWord?: string;
    rawInputWord?: string;
  } | null
): string {
  if (!item) return "";
  return item.word || item.normalizedWord || item.rawWord || item.rawInputWord || "";
}

