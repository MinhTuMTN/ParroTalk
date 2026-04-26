"use client";

import { useEffect, useState } from "react";
import { lessonService } from "@/src/services/lessonService";
import type { Lesson, LessonStatus, Segment } from "@/src/types/lesson";

type LessonForm = {
  title: string;
  source: string;
  duration: number;
  status: LessonStatus;
};

const makeSegmentId = () =>
  `seg-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`;

export function useLessonDetail(id: string) {
  const [lesson, setLesson] = useState<Lesson | null>(null);
  const [form, setForm] = useState<LessonForm | null>(null);
  const [segments, setSegments] = useState<Segment[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    const loadLesson = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await lessonService.getLessonById(id);
        if (!mounted) return;
        setLesson(data);
        setForm({
          title: data.title,
          source: data.source,
          duration: data.duration,
          status: data.status,
        });
        setSegments(data.segments);
      } catch {
        if (!mounted) return;
        setError("Failed to load lesson details.");
      } finally {
        if (!mounted) return;
        setLoading(false);
      }
    };

    void loadLesson();

    return () => {
      mounted = false;
    };
  }, [id]);

  const clearMessages = () => {
    setError(null);
    setSuccess(null);
  };

  const updateForm = <K extends keyof LessonForm>(key: K, value: LessonForm[K]) => {
    setForm((current) => (current ? { ...current, [key]: value } : current));
  };

  const updateSegment = (segmentId: string, patch: Partial<Segment>) => {
    setSegments((current) =>
      current.map((segment) =>
        segment.id === segmentId ? { ...segment, ...patch } : segment,
      ),
    );
  };

  const addSegment = (insertAfterId?: string) => {
    setSegments((current) => {
      const insertAt = insertAfterId
        ? Math.max(
            0,
            current.findIndex((segment) => segment.id === insertAfterId) + 1,
          )
        : current.length;

      const prev = current[insertAt - 1];
      const next = current[insertAt];
      const startTime = prev ? Number((prev.endTime + 0.1).toFixed(2)) : 0;
      const endTime = next
        ? Number(Math.max(startTime + 0.1, next.startTime - 0.1).toFixed(2))
        : Number((startTime + 4).toFixed(2));

      const nextState = [...current];
      nextState.splice(insertAt, 0, {
        id: makeSegmentId(),
        text: "",
        startTime,
        endTime,
      });
      return nextState;
    });
  };

  const removeSegment = (segmentId: string) => {
    setSegments((current) => current.filter((segment) => segment.id !== segmentId));
  };

  const splitSegment = (segmentId: string, cursorPosition?: number) => {
    setSegments((current) => {
      const index = current.findIndex((segment) => segment.id === segmentId);
      if (index < 0) return current;

      const currentSegment = current[index];
      const splitTextAt = cursorPosition ?? currentSegment.text.length;
      const safeSplit = Math.max(0, Math.min(splitTextAt, currentSegment.text.length));
      const leftText = currentSegment.text.slice(0, safeSplit).trimEnd();
      const rightText = currentSegment.text.slice(safeSplit).trimStart();
      const middle = Number(
        ((currentSegment.startTime + currentSegment.endTime) / 2).toFixed(2),
      );

      const first = {
        ...currentSegment,
        text: leftText || currentSegment.text,
        endTime: middle,
      };

      const second: Segment = {
        id: makeSegmentId(),
        text: rightText,
        startTime: middle,
        endTime: currentSegment.endTime,
      };

      const nextState = [...current];
      nextState.splice(index, 1, first, second);
      return nextState;
    });
  };

  const saveChanges = async () => {
    if (!form) return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const updatedLesson = await lessonService.updateLesson(id, {
        title: form.title,
        source: form.source,
        status: form.status,
      });
      const updatedSegments = await lessonService.updateSegments(id, segments);
      setLesson({ ...updatedLesson, segments: updatedSegments });
      setSegments(updatedSegments);
      setSuccess("Lesson changes saved.");
    } catch {
      setError("Failed to save lesson changes.");
    } finally {
      setSaving(false);
    }
  };

  const deleteLesson = async () => {
    setDeleting(true);
    setError(null);
    setSuccess(null);
    try {
      await lessonService.deleteLesson(id);
      setSuccess("Lesson deleted.");
      return true;
    } catch {
      setError("Failed to delete lesson.");
      return false;
    } finally {
      setDeleting(false);
    }
  };

  return {
    lesson,
    form,
    segments,
    loading,
    saving,
    deleting,
    error,
    success,
    updateForm,
    updateSegment,
    addSegment,
    removeSegment,
    splitSegment,
    saveChanges,
    deleteLesson,
    clearMessages,
  };
}
