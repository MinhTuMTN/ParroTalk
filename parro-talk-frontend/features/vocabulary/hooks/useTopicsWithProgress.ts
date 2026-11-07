import { useMemo } from "react";
import { useVocabularyTopics } from "./index";
import { enrichTopic, type TopicWithProgress } from "../lib/topicMeta";

/** Topics from the API, enriched with category / level / progress metadata. */
export function useTopicsWithProgress(): { topics: TopicWithProgress[]; loading: boolean } {
  const { topics, loading } = useVocabularyTopics();
  const enriched = useMemo(() => topics.map(enrichTopic), [topics]);
  return { topics: enriched, loading };
}

/**
 * TODO(backend): replace with a real `/vocabulary/stats` endpoint.
 */
export function useVocabularyStats() {
  return {
    streak: 7,
    reviewCount: 18,
    reviewMinutes: 5,
    totalLearned: 248,
    learning: 62,
    mastered: 186,
    weeklyGain: 24,
    wordGoal: 400,
    userLevel: "B1" as const,
  };
}
