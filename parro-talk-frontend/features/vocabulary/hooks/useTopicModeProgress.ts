"use client";

import { useState, useCallback } from "react";

export type LearningModeId =
  | "flashcard"
  | "multiple-choice"
  | "type-word"
  | "fill-blank"
  | "listen-type"
  | "pronunciation";

export type ModeProgressStatus = "not-started" | "in-progress" | "completed";

export interface ModeProgressItem {
  id: LearningModeId;
  status: ModeProgressStatus;
  completed: number;
  total: number;
  percent: number;
  buttonLabel: "Start" | "Continue" | "Review";
}

export type TopicModeProgressMap = Record<LearningModeId, ModeProgressItem>;

const MODE_DEFAULT_TOTALS: Record<LearningModeId, number> = {
  flashcard: 16,
  "multiple-choice": 12,
  "type-word": 12,
  "fill-blank": 10,
  "listen-type": 8,
  pronunciation: 12,
};

function getStorageKey(topicId: string) {
  return `parrotalk_mode_progress_${topicId.toLowerCase().replace(/\s+/g, "_")}`;
}

function createInitialProgressMap(topicId: string, wordCount: number): TopicModeProgressMap {
  const initial = {} as TopicModeProgressMap;
  const modeKeys: LearningModeId[] = [
    "flashcard",
    "multiple-choice",
    "type-word",
    "fill-blank",
    "listen-type",
    "pronunciation",
  ];

  modeKeys.forEach((mode) => {
    const total = Math.min(MODE_DEFAULT_TOTALS[mode], Math.max(5, wordCount));
    initial[mode] = {
      id: mode,
      status: "not-started",
      completed: 0,
      total,
      percent: 0,
      buttonLabel: "Start",
    };
  });

  if (!topicId || typeof window === "undefined") {
    return initial;
  }

  try {
    const stored = localStorage.getItem(getStorageKey(topicId));
    if (stored) {
      const parsed = JSON.parse(stored) as Record<
        string,
        { completed: number; total?: number; status?: ModeProgressStatus }
      >;

      modeKeys.forEach((mode) => {
        const data = parsed[mode];
        if (data && typeof data.completed === "number") {
          const total = data.total || initial[mode].total;
          const completed = Math.min(data.completed, total);
          let status: ModeProgressStatus = "not-started";

          if (completed >= total && total > 0) {
            status = "completed";
          } else if (completed > 0) {
            status = "in-progress";
          }

          const percent = total > 0 ? Math.round((completed / total) * 100) : 0;
          const buttonLabel =
            status === "completed"
              ? "Review"
              : status === "in-progress"
              ? "Continue"
              : "Start";

          initial[mode] = {
            id: mode,
            status,
            completed,
            total,
            percent,
            buttonLabel,
          };
        }
      });
    }
  } catch (err) {
    console.error("Failed to load mode progress from storage", err);
  }

  return initial;
}

export function useTopicModeProgress(topicId: string, wordCount: number = 40) {
  const [progressMap, setProgressMap] = useState<TopicModeProgressMap>(() =>
    createInitialProgressMap(topicId, wordCount)
  );

  const [prevParams, setPrevParams] = useState({ topicId, wordCount });
  if (prevParams.topicId !== topicId || prevParams.wordCount !== wordCount) {
    setPrevParams({ topicId, wordCount });
    setProgressMap(createInitialProgressMap(topicId, wordCount));
  }

  // Method to update mode progress explicitly
  const updateProgress = useCallback(
    (mode: LearningModeId, completed: number, customTotal?: number) => {
      if (!topicId || typeof window === "undefined") return;

      setProgressMap((prev) => {
        const item = prev[mode];
        const total = customTotal || item.total;
        const boundedCompleted = Math.min(Math.max(0, completed), total);

        let status: ModeProgressStatus = "not-started";
        if (boundedCompleted >= total && total > 0) {
          status = "completed";
        } else if (boundedCompleted > 0) {
          status = "in-progress";
        }

        const percent = total > 0 ? Math.round((boundedCompleted / total) * 100) : 0;
        const buttonLabel =
          status === "completed"
            ? "Review"
            : status === "in-progress"
            ? "Continue"
            : "Start";

        const nextItem: ModeProgressItem = {
          id: mode,
          status,
          completed: boundedCompleted,
          total,
          percent,
          buttonLabel,
        };

        const nextMap = { ...prev, [mode]: nextItem };

        try {
          const serializable: Record<
            string,
            { completed: number; total: number; status: ModeProgressStatus }
          > = {};
          Object.entries(nextMap).forEach(([k, v]) => {
            serializable[k] = {
              completed: v.completed,
              total: v.total,
              status: v.status,
            };
          });
          localStorage.setItem(getStorageKey(topicId), JSON.stringify(serializable));
        } catch (err) {
          console.error("Failed to save mode progress", err);
        }

        return nextMap;
      });
    },
    [topicId]
  );

  return { progressMap, updateProgress };
}
