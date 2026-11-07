"use client";

import React, { useEffect, useState, useMemo, useCallback } from "react";
import { useParams } from "next/navigation";
import {
  TopicDetailHero,
  TopicProgressCard,
  RecommendedLearningCard,
  DifferentWaysToLearn,
  SmartReviewCard,
  TopicWordList,
  TopicSidebar,
  type WordItemWithStatus,
} from "@/features/vocabulary/components/topic-detail";
import { FooterNote } from "@/features/vocabulary/components/shared";
import * as api from "@/features/vocabulary/services/api";
import { getLocalWordStatusesForTopic } from "@/features/vocabulary/services/vocabularyPracticeService";
import { enrichTopic } from "@/features/vocabulary/lib/topicMeta";

export default function TopicDetailPage() {
  const params = useParams();
  const rawId = Array.isArray(params.id) ? params.id[0] : params.id;
  const topicId = decodeURIComponent(rawId || "");

  const [topicData, setTopicData] = useState<any>(null);
  const [topicLoading, setTopicLoading] = useState(true);
  const [topicError, setTopicError] = useState<any>(null);

  const fetchTopic = useCallback(() => {
    let active = true;
    setTopicLoading(true);
    setTopicError(null);
    api.getTopicSummary(topicId)
       .then(res => { if (active) setTopicData(res); })
       .catch(err => { if (active) setTopicError(err?.response?.status === 404 ? 'not-found' : 'error'); })
       .finally(() => { if (active) setTopicLoading(false); });
    return () => { active = false; };
  }, [topicId]);

  useEffect(() => fetchTopic(), [fetchTopic]);

  const [page, setPage] = useState(0);
  const size = 6;
  
  // Cache to store pages: { pageIndex: WordItemWithStatus[] }
  const wordCache = React.useRef<Record<number, WordItemWithStatus[]>>({});
  
  const [wordsData, setWordsData] = useState<any>(null);
  const [wordsLoading, setWordsLoading] = useState(true);
  const [wordsError, setWordsError] = useState<any>(null);
  const [totalPages, setTotalPages] = useState<number>(1);

  const fetchWords = useCallback((targetPage: number, isPrefetch = false) => {
    if (wordCache.current[targetPage]) {
      if (!isPrefetch) {
        setWordsData({ content: wordCache.current[targetPage] });
        setWordsLoading(false);
        setWordsError(null);
      }
      return;
    }

    if (!isPrefetch) {
      setWordsLoading(true);
      setWordsError(null);
    }

    api.getTopicWords(topicId, { page: targetPage, size })
       .then(res => {
         const parsedWords = (res.content || []).map((w: any) => ({
           ...w,
           status: w.status ? w.status.toLowerCase() : null
         }));
         wordCache.current[targetPage] = parsedWords;
         
         if (!isPrefetch) {
           setTotalPages(res.totalPages || 1);
           setWordsData({ content: parsedWords });
         }
       })
       .catch(err => {
         if (!isPrefetch) setWordsError(true);
       })
       .finally(() => {
         if (!isPrefetch) setWordsLoading(false);
       });
  }, [topicId, size]);

  // Fetch current page
  useEffect(() => {
    fetchWords(page, false);
  }, [page, fetchWords]);

  // Prefetch next 3 pages
  useEffect(() => {
    if (totalPages > 1) {
      for (let i = 1; i <= 3; i++) {
        const prefetchPage = page + i;
        if (prefetchPage < totalPages) {
          fetchWords(prefetchPage, true);
        }
      }
    }
  }, [page, totalPages, fetchWords]);

  if (topicLoading) {
    return (
      <div className="min-h-screen bg-ink-50 p-8 flex items-center justify-center">
        <div className="text-center space-y-3">
          <div className="w-10 h-10 border-3 border-brand-500 border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="text-sm font-semibold text-ink-500">Loading topic details…</p>
        </div>
      </div>
    );
  }

  if (topicError === 'not-found') {
    return (
      <div className="min-h-screen bg-ink-50 p-8 flex items-center justify-center">
        <div className="text-center space-y-4 max-w-md">
          <h1 className="text-3xl font-bold text-ink-900">Topic Not Found</h1>
          <p className="text-ink-600">The topic you are looking for does not exist or has been removed.</p>
          <button onClick={() => window.history.back()} className="px-4 py-2 bg-brand-500 text-white rounded-lg hover:bg-brand-600 transition-colors">Go Back</button>
        </div>
      </div>
    );
  }

  if (topicError) {
    return (
      <div className="min-h-screen bg-ink-50 p-8 flex items-center justify-center">
        <div className="text-center space-y-4 max-w-md">
          <h1 className="text-3xl font-bold text-red-600">Error Loading Topic</h1>
          <p className="text-ink-600">We encountered an error while loading the topic details.</p>
          <button onClick={() => fetchTopic()} className="px-4 py-2 bg-brand-500 text-white rounded-lg hover:bg-brand-600 transition-colors">Retry</button>
        </div>
      </div>
    );
  }

  const localStatuses = {
    ...getLocalWordStatusesForTopic(topicId),
    ...getLocalWordStatusesForTopic(topicData?.name || ""),
  };
  const rawWords: WordItemWithStatus[] = wordsData?.content || [];
  const apiWords: WordItemWithStatus[] = rawWords.map((w) => {
    const normalized = w.word?.trim().toLowerCase() || "";
    const localStatus = localStatuses[normalized];
    const mergedStatus =
      w.status === "mastered" || localStatus === "mastered"
        ? "mastered"
        : w.status === "learning" || localStatus === "learning"
        ? "learning"
        : w.status || "new";
    return {
      ...w,
      status: mergedStatus,
    };
  });

  const localLearnedCount = Object.keys(localStatuses).length;
  const learned = Math.max(
    topicData?.learned ?? topicData?.learnedWordCount ?? 0,
    localLearnedCount
  );
  const enrichedTopicData = enrichTopic(
    {
      ...topicData,
      learnedWordCount: learned,
    },
    0
  );

  const nextWordToLearn =
    apiWords.find((w) => w.status === "new" || !w.status)?.word ||
    apiWords.find((w) => w.status === "learning")?.word ||
    apiWords[0]?.word ||
    "Itinerary";

  return (
    <div className="relative min-h-full bg-ink-50 text-ink-800">
      <div className="pointer-events-none absolute inset-x-0 top-0 h-80 bg-gradient-to-b from-brand-50 to-transparent" />

      <div className="relative mx-auto max-w-[1100px] space-y-8 px-4 py-8 sm:px-8 sm:py-10">
        <TopicDetailHero topic={enrichedTopicData} />
        <TopicProgressCard topic={enrichedTopicData} />
        <RecommendedLearningCard topic={enrichedTopicData} nextWord={nextWordToLearn} />
        <DifferentWaysToLearn topic={enrichedTopicData} />
        <SmartReviewCard topic={enrichedTopicData} />

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start pt-2">
          <div className="lg:col-span-2">
            {wordsError && (
               <div className="mb-4 p-4 bg-red-50 text-red-600 rounded-lg flex justify-between items-center shadow-sm">
                 <span>Failed to load words.</span>
                 <button onClick={() => fetchWords(page, false)} className="text-sm font-semibold underline">Retry</button>
               </div>
            )}
            <TopicWordList
              words={apiWords}
              totalWords={enrichedTopicData?.wordCount || 0}
              loading={wordsLoading}
              page={page}
              totalPages={totalPages}
              onPrevPage={() => setPage(p => Math.max(0, p - 1))}
              onNextPage={() => setPage(p => p + 1)}
            />
          </div>

          <div className="lg:col-span-1">
            <TopicSidebar topic={enrichedTopicData} nextWord={nextWordToLearn} />
          </div>
        </div>

        <FooterNote />
      </div>
    </div>
  );
}
