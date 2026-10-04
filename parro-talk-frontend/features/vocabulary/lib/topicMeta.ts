import type { LucideIcon } from "lucide-react";
import {
  Briefcase,
  Plane,
  Wallet,
  Monitor,
  GraduationCap,
  Heart,
  Newspaper,
  Globe,
  MessageCircle,
  Utensils,
  Coffee,
} from "lucide-react";
import type { VocabularyTopicDto } from "../types";

// ─── Categories ──────────────────────────────────────────────────────────────

export type TopicCategory =
  | "Business"
  | "Travel"
  | "Finance"
  | "Technology"
  | "Education"
  | "Emotions"
  | "Media"
  | "General";

export type Tint = "brand" | "coral" | "ocean" | "honey" | "plum" | "ink";

/** Tailwind classes per tint — kept literal so Tailwind can detect them. */
export const TINT_CLASSES: Record<Tint, string> = {
  brand: "bg-brand-50 text-brand-600 ring-brand-100",
  coral: "bg-coral-50 text-coral-600 ring-coral-100",
  ocean: "bg-ocean-50 text-ocean-600 ring-ocean-100",
  honey: "bg-honey-50 text-honey-600 ring-honey-100",
  plum: "bg-plum-50 text-plum-600 ring-plum-100",
  ink: "bg-ink-100 text-ink-600 ring-ink-200",
};

export interface CategoryMeta {
  label: TopicCategory;
  icon: LucideIcon;
  tint: Tint;
  keywords: string[];
}

export const CATEGORIES: CategoryMeta[] = [
  { label: "Business", icon: Briefcase, tint: "ocean", keywords: ["work", "career", "business", "office", "job", "idea", "innovation", "meeting"] },
  { label: "Travel", icon: Plane, tint: "brand", keywords: ["travel", "adventure", "trip", "city", "airport", "hotel", "around"] },
  { label: "Finance", icon: Wallet, tint: "honey", keywords: ["money", "bank", "finance", "shopping", "price"] },
  { label: "Technology", icon: Monitor, tint: "ink", keywords: ["tech", "digital", "computer", "internet", "device"] },
  { label: "Education", icon: GraduationCap, tint: "plum", keywords: ["learn", "school", "campus", "education", "study"] },
  { label: "Emotions", icon: Heart, tint: "coral", keywords: ["feeling", "emotion", "mood", "love"] },
  { label: "Media", icon: Newspaper, tint: "ink", keywords: ["news", "media", "affair", "film", "music"] },
  { label: "General", icon: Globe, tint: "brand", keywords: [] },
];

/** A few name-specific icons make common topics feel more personal. */
const ICON_OVERRIDES: { match: string; icon: LucideIcon; tint: Tint }[] = [
  { match: "conversation", icon: MessageCircle, tint: "ink" },
  { match: "food", icon: Utensils, tint: "coral" },
  { match: "dining", icon: Utensils, tint: "coral" },
  { match: "coffee", icon: Coffee, tint: "coral" },
];

// ─── Levels ──────────────────────────────────────────────────────────────────

export const CEFR_LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"] as const;
export type CefrLevel = (typeof CEFR_LEVELS)[number];

export const CEFR_LABELS: Record<CefrLevel, string> = {
  A1: "Starter",
  A2: "Beginner",
  B1: "Intermediate",
  B2: "Upper-Int.",
  C1: "Advanced",
  C2: "Proficient",
};

// ─── Progress ────────────────────────────────────────────────────────────────

export type TopicStatus = "not-started" | "in-progress" | "completed";

export interface TopicWithProgress extends VocabularyTopicDto {
  category: TopicCategory;
  Icon: LucideIcon;
  tint: Tint;
  level: CefrLevel;
  progress: number; // 0 – 100
  learned: number;
  remaining: number;
  status: TopicStatus;
  isCurrent: boolean;
}

function hash(str: string): number {
  let h = 0;
  for (let i = 0; i < str.length; i++) h = (h * 31 + str.charCodeAt(i)) | 0;
  return Math.abs(h);
}

function inferCategory(name: string): CategoryMeta {
  const n = name.toLowerCase();
  return (
    CATEGORIES.find((c) => c.keywords.some((k) => n.includes(k))) ??
    CATEGORIES[CATEGORIES.length - 1]
  );
}

/**
 * TODO(backend): category, level and per-user progress are not yet returned by
 * the API. Until then they are derived deterministically so the UI is stable.
 */
const MOCK_PROGRESS_BY_INDEX = [60, 79, 100];

export function enrichTopic(topic: VocabularyTopicDto, index: number): TopicWithProgress {
  const h = hash(topic.id || topic.name);
  const category = inferCategory(topic.name);
  const override = ICON_OVERRIDES.find((o) => topic.name.toLowerCase().includes(o.match));

  let learned = topic.learnedWordCount;
  let progress = 0;
  
  if (learned === undefined) {
    progress = MOCK_PROGRESS_BY_INDEX[index];
    if (progress === undefined) {
      const bucket = h % 6;
      progress = bucket === 0 ? 100 : bucket === 1 ? 20 + (h % 60) : 0;
    }
    learned = Math.round((progress / 100) * topic.wordCount);
  } else {
    progress = topic.wordCount > 0 ? Math.min(100, Math.round((learned / topic.wordCount) * 100)) : 0;
  }
  const status: TopicStatus =
    progress >= 100 ? "completed" : progress > 0 ? "in-progress" : "not-started";

  return {
    ...topic,
    category: category.label,
    Icon: override?.icon ?? category.icon,
    tint: override?.tint ?? category.tint,
    level: CEFR_LEVELS[1 + (h % 4)],
    progress,
    learned,
    remaining: topic.wordCount - learned,
    status,
    isCurrent: index === 0,
  };
}

// ─── Routes ──────────────────────────────────────────────────────────────────

export const topicHref = (id: string) => `/vocabulary/topics/${id}`;
