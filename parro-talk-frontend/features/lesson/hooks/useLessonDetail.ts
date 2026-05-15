"use client";

import { useEffect, useMemo, useState } from "react";

import { lessonService } from "@/features/lesson/services/lessonService";
import type { Lesson, LessonStatus, Segment } from "@/features/lesson/types/lesson";

type LessonForm = {
  title: string;
  source: string;
  duration: number;
  status: LessonStatus;
  categoryIds: string[];
};

const makeSegmentId = () =>
  `seg-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`;

export function useLessonDetail(id: string) {
  const [lesson, setLesson] = useState<Lesson | null>(null);
  const [form, setForm] = useState<LessonForm | null>(null);
  const [segments, setSegments] = useState<Segment[]>([]);
  const [loading, setLoading] = useState(true);
  const [originalSegments, setOriginalSegments] = useState<Segment[]>([]);

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
        const data = await lessonService.getAdminLessonById(id);
        if (!mounted) return;
        setLesson(data);
        setForm({
          title: data.title,
          source: data.source,
          duration: data.duration,
          status: data.status,
          categoryIds: data.categories.map((category) => category.id),
        });
        setSegments(data.segments);
        setOriginalSegments(data.segments);

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

    const hasSegmentChanges = useMemo(() => {
      if (segments.length !== originalSegments.length) return true;

      return segments.some((segment, index) => {
        const original = originalSegments[index];
        if (!original) return true;
        return (
          segment.id !== original.id ||
          segment.text !== original.text ||
          segment.startTime !== original.startTime ||
          segment.endTime !== original.endTime
        );
      });
    }, [originalSegments, segments]);

    const getChangedSegmentsForSave = () => {
      const currentOrderMap = new Map(segments.map((segment, index) => [segment.id, index]));

      return segments.filter((segment, index) => {
        if (segment.id.startsWith("seg-")) return true;
        const originalIndex = originalSegments.findIndex((item) => item.id === segment.id);
        if (originalIndex < 0) return true;

        const original = originalSegments[originalIndex];
        const orderChanged = currentOrderMap.get(segment.id) !== originalIndex;

        return (
          orderChanged ||
          segment.text !== original.text ||
          segment.startTime !== original.startTime ||
          segment.endTime !== original.endTime
        );
      });
    };


  const saveChanges = async () => {

    if (!form) return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
            const changedSegments = hasSegmentChanges ? getChangedSegmentsForSave() : [];
            const deletedSegmentIds = originalSegments
              .filter((original) => !segments.some((segment) => segment.id === original.id))
              .map((segment) => segment.id);

            const [updatedLesson, updatedSegments] = await Promise.all([

        lessonService.updateLesson(id, {
          title: form.title,
          source: form.source,
          status: form.status,
          categoryIds: form.categoryIds,
        }),
                hasSegmentChanges
          ? lessonService.updateSegments(id, changedSegments, deletedSegmentIds)
          : Promise.resolve(segments),

      ]);
      setLesson({ ...updatedLesson, segments: updatedSegments });
      setSegments(updatedSegments);
      setOriginalSegments(updatedSegments);
      setSuccess(hasSegmentChanges ? "Lesson and segments saved." : "Lesson changes saved.");

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
    hasSegmentChanges,
    saveChanges,

    deleteLesson,
    clearMessages,
  };
}

