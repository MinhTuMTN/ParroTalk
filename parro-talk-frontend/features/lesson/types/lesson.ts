export enum LessonStatus {
  PUBLISHED = "PUBLISHED",
  HIDDEN = "HIDDEN",
}

export type Segment = {
  id: string;
  text: string;
  startTime: number;
  endTime: number;
};

export type LessonCategory = {
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
  segments: Segment[];
  createdAt: string;
};

export type LessonFilter = "all" | LessonStatus;
