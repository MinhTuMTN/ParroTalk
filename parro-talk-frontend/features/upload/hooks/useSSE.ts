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
    if (!lessonId) {
      console.warn('useSSE: No lessonId provided, skipping connection.');
      return;
    }

    console.log(`useSSE: Initializing connection for lessonId: ${lessonId}`);
    setState({
      status: 'CONNECTING',
      progress: 0,
      step: 'Establishing connection...',
    });

    // Normalize URL: remove potential double slashes
    const baseUrl = API_URL.endsWith('/') ? API_URL.slice(0, -1) : API_URL;
    const sseUrl = `${baseUrl}/lessons/sse/${lessonId}`;
    
    console.log(`useSSE: Connecting to ${sseUrl}`);
    const eventSource = new EventSource(sseUrl);

    eventSource.onopen = () => {
      console.log('useSSE: SSE connection opened.');
    };

    eventSource.onmessage = (event) => {
      console.log('useSSE: Received message:', event.data);
      if (event.data === 'connected') {
        setState((prev) => ({ 
          ...prev, 
          status: 'PROCESSING', 
          step: prev.step === 'Establishing connection...' ? 'Connected to stream' : prev.step 
        }));
        return;
      }
      try {
        const data = JSON.parse(event.data);
        console.log('useSSE: Parsed data:', data);
        if (data.status) {
          setState((prev) => ({
            ...prev,
            status: data.status,
            progress: data.progress ?? prev.progress,
            step: data.step ?? prev.step,
          }));

          if (data.status === 'DONE' || data.status === 'FAILED') {
            console.log(`useSSE: Connection closing due to status: ${data.status}`);
            eventSource.close();
          }
        }
      } catch (err) {
        console.error('useSSE: Failed to parse SSE message:', err, 'Raw data:', event.data);
      }
    };

    eventSource.onerror = (err) => {
      console.error('useSSE: SSE connection error:', err);
      setState((prev) => ({
        ...prev,
        status: 'FAILED',
        error: 'Failed to connect to process stream. Please refresh.',
      }));
      eventSource.close();
    };

    return () => {
      console.log('useSSE: Cleaning up connection.');
      eventSource.close();
      setState((prev) => ({ ...prev, status: 'IDLE' }));
    };
  }, [lessonId]);

  return state;
}

