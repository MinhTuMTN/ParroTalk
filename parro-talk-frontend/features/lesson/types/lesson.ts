export enum LessonStatus {
  PUBLISHED = "PUBLISHED",
  HIDDEN = "HIDDEN",
}

export type Segment = {
  id: string;
  text: string;
  startTime: number;
  endTime: number;
  translation?: SegmentTranslation | null;
};

export type SegmentTranslation = {
  targetLanguage: string;
  translatedText: string;
};

export type TranslationSummary = {
  targetLanguage: string;
  status: "NOT_STARTED" | "PARTIAL" | "COMPLETED";
  translatedCount: number;
  totalSegments: number;
};

export type LessonCategory = {
  id: string;
  name: string;
};

export type LessonTag = {
  id: string;
  name: string;
};

export type Lesson = {
  id: string;
  title: string;
  source: string;
  duration: number;
  status: LessonStatus;
  categories: LessonCategory[];
  tags: LessonTag[];
  segments: Segment[];
  translationSummary?: TranslationSummary;
  createdAt: string;
  updatedAt: string;
};

export type LessonFilter = "all" | LessonStatus;
