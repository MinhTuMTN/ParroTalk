import { useState, useEffect } from 'react';
import { API_URL } from '@/lib/axios';

type SSEStatus = 'PROCESSING' | 'DONE' | 'FAILED' | 'CONNECTING' | 'IDLE';

interface SSEState {
  status: SSEStatus;
  progress: number;
  step: string;
  error?: string;
}

export function useSSE(lessonId: string | null) {
  const [state, setState] = useState<SSEState>({
    status: 'IDLE',
    progress: 0,
    step: 'Initializing...',
  });

  useEffect(() => {
    if (!lessonId) return;

    setState({
      status: 'CONNECTING',
      progress: 0,
      step: 'Connecting to server...',
    });

    const eventSource = new EventSource(`${API_URL}/lessons/sse/${lessonId}`);

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.status) {
          setState((prev) => ({
            ...prev,
            status: data.status,
            progress: data.progress ?? prev.progress,
            step: data.step ?? prev.step,
          }));

          if (data.status === 'DONE' || data.status === 'FAILED') {
            eventSource.close();
          }
        }
      } catch (err) {
        console.error('Failed to parse SSE message:', err);
      }
    };

    eventSource.onerror = (err) => {
      console.error('SSE connection error:', err);
      setState((prev) => ({
        ...prev,
        status: 'FAILED',
        error: 'Connection dropped. Please try again.',
      }));
      eventSource.close();
    };

    return () => {
      eventSource.close();
      setState((prev) => ({ ...prev, status: 'IDLE' }));
    };
  }, [lessonId]);

  return state;
}
