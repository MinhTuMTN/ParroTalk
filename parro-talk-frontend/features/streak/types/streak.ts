export type WeeklyProgressDay = {
  day: string;
  date: string;
  studied: boolean;
};

export type CalendarDay = {
  date: string;
  studied: boolean;
};

export type StreakStatistics = {
  totalStudyDays: number;
  totalStudyMinutes: number;
  lessonsCompleted: number;
  averageDailyStudyMinutes: number;
  currentStreak: number;
  longestStreak: number;
};

export type Achievement = {
  code: string;
  title: string;
  achieved: boolean;
  achievedAt: string | null;
  progress: number | null;
  target: number | null;
};

export type Motivation = {
  title: string;
  quote: string;
  author: string;
  warningMessage: string | null;
};

export type UserStreakResponse = {
  currentStreak: number;
  longestStreak: number;
  longestStreakAchievedAt: string | null;
  hasStudiedToday: boolean;
  weeklyGoalDays: number;
  weeklyCompletedDays: number;
  weeklyProgress: WeeklyProgressDay[];
  calendarYear: number;
  calendarYears: number[];
  calendar: CalendarDay[];
  statistics: StreakStatistics;
  achievements: Achievement[];
  motivation: Motivation;
};
