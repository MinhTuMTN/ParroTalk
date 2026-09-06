import type { WeeklyProgressDay } from "@/features/streak/types/streak";
import type { Lesson } from "@/features/lesson/services/lessonService";

export type SourceType = "BBC" | "6 MIN" | "TED" | "VOA" | "OTHER" | string;

export interface ContinueLearningItem {
  id: string;
  title: string;
  source: SourceType;
  sourceColor?: string;
  durationMinutes: number;
  progressPercent: number; // 0 - 100
  thumbnailUrl?: string;
  lastPracticedAt?: string;
}

export type LessonProcessingStatus = "READY" | "PROCESSING" | "FAILED";

export interface UploadedLessonItem {
  id: string;
  title: string;
  status: LessonProcessingStatus;
  durationMinutes: number;
  createdAt: string;
  thumbnailUrl?: string;
  progress?: number;
  errorMessage?: string;
}

export type TopicGradientTheme = "emerald" | "indigo" | "coral" | "amber" | "blue" | "purple";

export interface TopicLessonItem {
  id: string;
  title: string;
  topicBadge?: string;
  lessonNumber?: string;
  durationMinutes: number;
  gradientTheme?: TopicGradientTheme;
  thumbnailUrl?: string;
  categoryName?: string;
  source?: string;
}

export interface StreakInfo {
  currentStreak: number;
  longestStreak: number;
  hasStudiedToday: boolean;
  weeklyProgress?: WeeklyProgressDay[];
}

export interface CategoryChipItem {
  id: string;
  label: string;
  icon?: string;
  isSelected?: boolean;
}

export interface ProcessingJobItem {
  id: string;
  title: string;
  progressPercent: number;
  status: "PENDING" | "PROCESSING" | "DONE" | "FAILED";
  startedAt?: string;
  duration?: number;
  errorMessage?: string;
}

export type LibraryActiveTab = "explore" | "my-uploads" | "history" | "library" | "my-lessons";

export interface LibraryFilterState {
  searchQuery: string;
  selectedCategoryId: string;
  activeTab: LibraryActiveTab;
  page: number;
}

export interface CuratedShelf {
  id: string;
  title: string;
  subtitle?: string;
  iconName?: string;
  accentColor?: string;
  gradientTheme?: TopicGradientTheme;
  seeAllHref?: string;
  items: TopicLessonItem[];
}

export interface LibrarySection {
  id: string;
  title: string;
  type: "IN_PROGRESS" | "USER_PROCESSING" | "USER_UPLOADED" | "CATEGORY" | "PROGRAM" | string;
  items: Lesson[];
}
