export type LessonStatus = "published" | "hidden";

export type Segment = {
  id: string;
  text: string;
  startTime: number;
  endTime: number;
};

export type Lesson = {
  id: string;
  title: string;
  source: string;
  duration: number;
  status: LessonStatus;
  segments: Segment[];
  createdAt: string;
};

export type LessonFilter = "all" | LessonStatus;