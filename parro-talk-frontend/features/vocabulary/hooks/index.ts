import { useState, useEffect, useCallback } from 'react';
import * as api from '../services/api';
import { VocabularyTopicDto, VocabularySummaryDto, VocabularyDetailDto, PracticeQuestionDto } from '../types';

export const useVocabularyTopics = () => {
  const [topics, setTopics] = useState<VocabularyTopicDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.getTopics()
      .then(setTopics)
      .catch((err) => console.error('Failed to fetch topics', err))
      .finally(() => setLoading(false));
  }, []);

  return { topics, loading };
};

export const useVocabularySearch = () => {
  const [results, setResults] = useState<VocabularySummaryDto[]>([]);
  const [loading, setLoading] = useState(false);

  const search = useCallback(async (keyword: string, topic?: string) => {
    setLoading(true);
    try {
      const data = await api.searchVocabulary({ keyword, topic });
      setResults(data.content || []);
    } catch (err) {
      console.error('Search failed', err);
    } finally {
      setLoading(false);
    }
  }, []);

  return { results, loading, search };
};

export const useVocabularyDetail = (id: string) => {
  const [detail, setDetail] = useState<VocabularyDetailDto | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) {
      setLoading(false);
      return;
    }
    
    let isSubscribed = true;
    
    const fetchDetail = async () => {
      try {
        const data = await api.getVocabularyDetail(id);
        if (isSubscribed) {
          setDetail(data);
        }
      } catch (err) {
        console.error('Failed to fetch vocabulary detail', err);
      } finally {
        if (isSubscribed) {
          setLoading(false);
        }
      }
    };

    fetchDetail();

    return () => {
      isSubscribed = false;
    };
  }, [id]);

  return { detail, loading };
};

export const useBookmarkVocabulary = () => {
  const bookmark = useCallback(async (id: string) => {
    try {
      await api.bookmarkVocabulary(id);
      alert('TỪ VỰNG ĐÃ ĐƯỢC LƯU VÀO DANH SÁCH BẢN THÂN!');
    } catch {
      alert('Chức năng yêu cầu đăng nhập để lưu từ vựng!');
    }
  }, []);

  return { bookmark };
};

export const useVocabularyPractice = (topic?: string) => {
  const [questions, setQuestions] = useState<PracticeQuestionDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.getPracticeQuestions({ topic, count: 10 })
      .then(setQuestions)
      .catch((err) => console.error('Failed to fetch practice questions', err))
      .finally(() => setLoading(false));
  }, [topic]);

  return { questions, loading };
};