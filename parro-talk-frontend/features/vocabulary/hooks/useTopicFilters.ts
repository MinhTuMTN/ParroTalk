import { useDeferredValue, useMemo, useState } from "react";
import {
  CEFR_LEVELS,
  type CefrLevel,
  type TopicCategory,
  type TopicStatus,
  type TopicWithProgress,
} from "../lib/topicMeta";

export type CategoryFilter = "All" | TopicCategory;
export type StatusFilter = "all" | TopicStatus;
export type LevelFilter = "all" | CefrLevel;
export type SortOption = "recommended" | "az" | "words";

const STATUS_RANK: Record<TopicStatus, number> = { "in-progress": 0, "not-started": 1, completed: 2 };

export function useTopicFilters(topics: TopicWithProgress[], userLevel: CefrLevel) {
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState<CategoryFilter>("All");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [level, setLevel] = useState<LevelFilter>("all");
  const [sort, setSort] = useState<SortOption>("recommended");

  const deferredQuery = useDeferredValue(query);

  const filtered = useMemo(() => {
    const q = deferredQuery.trim().toLowerCase();
    const userIdx = CEFR_LEVELS.indexOf(userLevel);

    const result = topics.filter(
      (t) =>
        (category === "All" || t.category === category) &&
        (status === "all" || t.status === status) &&
        (level === "all" || t.level === level) &&
        (!q ||
          t.name.toLowerCase().includes(q) ||
          t.description?.toLowerCase().includes(q) ||
          t.category.toLowerCase().includes(q)),
    );

    return [...result].sort((a, b) => {
      if (sort === "az") return a.name.localeCompare(b.name);
      if (sort === "words") return b.wordCount - a.wordCount;
      const byStatus = STATUS_RANK[a.status] - STATUS_RANK[b.status];
      if (byStatus !== 0) return byStatus;
      const da = Math.abs(CEFR_LEVELS.indexOf(a.level) - userIdx);
      const db = Math.abs(CEFR_LEVELS.indexOf(b.level) - userIdx);
      return da - db;
    });
  }, [topics, deferredQuery, category, status, level, sort, userLevel]);

  const reset = () => {
    setQuery("");
    setCategory("All");
    setStatus("all");
    setLevel("all");
  };

  return {
    query, setQuery,
    category, setCategory,
    status, setStatus,
    level, setLevel,
    sort, setSort,
    filtered,
    reset,
  };
}
