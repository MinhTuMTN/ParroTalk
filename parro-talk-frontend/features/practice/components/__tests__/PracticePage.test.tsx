import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import PracticePage from "@/app/(main)/practice/[lessonId]/page";
import { DEFAULT_USER_SETTINGS } from "@/features/settings/types";

const mockPush = vi.fn();
const mockGetLessonById = vi.fn();
const mockGetLessonProgress = vi.fn();
const mockSubmitAnswer = vi.fn();
const mockFinalLessonSubmit = vi.fn();

vi.mock("next/navigation", () => ({
  useParams: () => ({ lessonId: "lesson-1" }),
  useRouter: () => ({ push: mockPush }),
}));

vi.mock("@/features/auth/hooks/useAuth", () => ({
  useAuth: () => ({ user: { fullName: "Tester" } }),
}));

vi.mock("@/hooks/useUI", () => ({
  useUI: () => ({ openMobileMenu: vi.fn() }),
}));

vi.mock("@/features/settings/hooks/useUserSettings", () => ({
  useUserSettings: () => ({
    settings: { ...DEFAULT_USER_SETTINGS, uiLanguage: "en" },
    updateSettings: vi.fn(),
  }),
}));

vi.mock("@/features/lesson/services/lessonService", () => ({
  lessonService: {
    getLessonById: (...args: unknown[]) => mockGetLessonById(...args),
    getLessonProgress: (...args: unknown[]) => mockGetLessonProgress(...args),
    submitAnswer: (...args: unknown[]) => mockSubmitAnswer(...args),
    finalLessonSubmit: (...args: unknown[]) => mockFinalLessonSubmit(...args),
    incrementReplay: vi.fn().mockResolvedValue(undefined),
    incrementHint: vi.fn().mockResolvedValue(undefined),
  },
}));

vi.mock("@/features/practice/components/VideoPlayer", () => ({
  default: () => <div data-testid="video-player" />,
}));

vi.mock("@/features/practice/components/TranscriptList", () => ({
  default: ({
    segments,
    onSelectSentence,
  }: {
    segments: Array<{ id: string | number }>;
    onSelectSentence: (idx: number) => void;
  }) => (
    <div data-testid="transcript-list">
      {segments.map((seg, idx) => (
        <button
          key={seg.id}
          type="button"
          onClick={() => onSelectSentence(idx)}
        >
          Jump {idx + 1}
        </button>
      ))}
    </div>
  ),
}));

vi.mock("@/features/practice/components/WordDictation", () => ({
  default: ({
    sentence,
    onSentenceComplete,
  }: {
    sentence: string;
    onSentenceComplete: () => void;
  }) => (
    <div data-testid="word-dictation">
      <span>{sentence}</span>
      <textarea aria-label="Dictation input" />
      <button type="button" onClick={onSentenceComplete}>
        Complete Current
      </button>
    </div>
  ),
}));

vi.mock("@/features/practice/components/FillInBlanksDictation", () => ({
  default: () => <div data-testid="fill-blanks-dictation" />,
}));

vi.mock("@/components/vocabulary/VocabularyDetailPopup", () => ({
  default: () => null,
}));

const sampleLesson = {
  id: "lesson-1",
  title: "Test Lesson",
  fileUrl: "https://example.com/audio.mp3",
  duration: 60,
  segments: [
    { id: "s1", text: "Sentence one.", translation: { translatedText: "Câu một." } },
    { id: "s2", text: "Sentence two.", translation: { translatedText: "Câu hai." } },
    { id: "s3", text: "Sentence three.", translation: { translatedText: "Câu ba." } },
  ],
};

describe("PracticePage UX flows", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    mockGetLessonById.mockResolvedValue(sampleLesson);
    mockGetLessonProgress.mockResolvedValue(null);
    mockSubmitAnswer.mockResolvedValue({});
    mockFinalLessonSubmit.mockResolvedValue({ score: 100, passed: true });
  });

  it("removes Skip & top-right Submit buttons, merges Skip into Next, and enforces 100% completion before submitting", async () => {
    render(<PracticePage />);

    await waitFor(() => {
      expect(screen.getByText("Sentence 1/3")).toBeInTheDocument();
    });

    // 1. No Skip button and no top-right Submit button
    expect(
      screen.queryByRole("button", { name: /skip sentence/i })
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /submit lesson/i })
    ).not.toBeInTheDocument();

    const nextBtn = screen.getByRole("button", { name: /next sentence/i });
    expect(nextBtn).toBeEnabled();

    // 2. Clicking Next on uncompleted sentence 1 marks it skipped & advances to sentence 2
    fireEvent.click(nextBtn);
    expect(screen.getByText("Sentence 2/3")).toBeInTheDocument();
    expect(
      screen.getByText(/1 sentence skipped in this session/i)
    ).toBeInTheDocument();

    const storedAfterSkip = JSON.parse(
      localStorage.getItem("parrotalk_progress_lesson-1_FULL") || "{}"
    );
    expect(storedAfterSkip.skipped).toEqual([0]);
    expect(storedAfterSkip.activeIndex).toBe(1);

    // Complete sentence 2 and continue to sentence 3
    fireEvent.click(screen.getByRole("button", { name: /complete current/i }));
    fireEvent.click(screen.getByRole("button", { name: /continue/i }));
    expect(screen.getByText("Sentence 3/3")).toBeInTheDocument();

    // 3. On last segment (3/3) with sentence 1 uncompleted: Next is still enabled and wraps to sentence 1
    const nextOnLast = screen.getByRole("button", { name: /next sentence/i });
    expect(nextOnLast).toBeEnabled();

    // Complete sentence 3 (last segment, but sentence 1 is still uncompleted)
    fireEvent.click(screen.getByRole("button", { name: /complete current/i }));

    // TranslationReview should NOT show Submit Lesson or "Submit Anyway" modal; should show Review Skipped (1)
    expect(
      screen.queryByRole("button", { name: /submit lesson/i })
    ).not.toBeInTheDocument();
    expect(screen.queryByText(/submit anyway/i)).not.toBeInTheDocument();
    const reviewSkippedBtn = screen.getByRole("button", {
      name: /review skipped \(1\)/i,
    });
    expect(reviewSkippedBtn).toBeInTheDocument();
    expect(
      screen.getByText(/complete all remaining sentences to submit/i)
    ).toBeInTheDocument();

    // Pressing Enter while on last segment with uncompleted sentence 1 jumps to sentence 1 instead of submitting
    fireEvent.keyDown(window, { key: "Enter" });
    expect(screen.getByText("Sentence 1/3")).toBeInTheDocument();
    expect(mockFinalLessonSubmit).not.toHaveBeenCalled();

    // 4. Complete the final remaining sentence (sentence 1/3): Next/TranslationReview immediately shows Submit Lesson!
    fireEvent.click(screen.getByRole("button", { name: /complete current/i }));
    const submitLessonBtn = screen.getByRole("button", {
      name: /submit lesson/i,
    });
    expect(submitLessonBtn).toBeInTheDocument();

    fireEvent.click(submitLessonBtn);

    await waitFor(() => {
      expect(mockFinalLessonSubmit).toHaveBeenCalledWith("lesson-1", "FULL");
      expect(mockPush).toHaveBeenCalledWith("/result");
    });
  });

  it("disables Next button only on last segment when all other sentences are completed, and enables it when earlier sentences remain uncompleted", async () => {
    render(<PracticePage />);

    await waitFor(() => {
      expect(screen.getByText("Sentence 1/3")).toBeInTheDocument();
    });

    // Skip sentence 1 -> 2, skip sentence 2 -> 3
    fireEvent.click(screen.getByRole("button", { name: /next sentence/i }));
    fireEvent.click(screen.getByRole("button", { name: /next sentence/i }));
    expect(screen.getByText("Sentence 3/3")).toBeInTheDocument();

    // On sentence 3 with 1 & 2 uncompleted: Next wraps to sentence 1 and marks 3 skipped
    const nextBtn = screen.getByRole("button", { name: /next sentence/i });
    expect(nextBtn).toBeEnabled();
    fireEvent.click(nextBtn);
    expect(screen.getByText("Sentence 1/3")).toBeInTheDocument();

    // Complete 1 -> advance to 2, complete 2 -> advance to 3
    fireEvent.click(screen.getByRole("button", { name: /complete current/i }));
    fireEvent.click(screen.getByRole("button", { name: /continue/i }));
    expect(screen.getByText("Sentence 2/3")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /complete current/i }));
    fireEvent.click(screen.getByRole("button", { name: /continue/i }));
    expect(screen.getByText("Sentence 3/3")).toBeInTheDocument();

    // Now on last segment (3/3) and sentences 1 & 2 are completed -> Next must be disabled
    expect(
      screen.getByRole("button", { name: /next sentence/i })
    ).toBeDisabled();
  });

  it("renders bottom Submit Lesson CTA when 100% completed with awaitingAdvance=false (e.g. reload or transcript jump)", async () => {
    mockGetLessonProgress.mockResolvedValue({
      currentSegmentId: "s2",
      draftSegments: [
        { segmentId: "s1", userAnswer: "Sentence one.", hintCount: 0, replayCount: 0, correct: true },
        { segmentId: "s2", userAnswer: "Sentence two.", hintCount: 0, replayCount: 0, correct: true },
        { segmentId: "s3", userAnswer: "Sentence three.", hintCount: 0, replayCount: 0, correct: true },
      ],
    });

    render(<PracticePage />);

    await waitFor(() => {
      expect(screen.getByText("Sentence 2/3")).toBeInTheDocument();
    });

    const submitBtn = screen.getByRole("button", { name: /submit lesson/i });
    expect(submitBtn).toBeInTheDocument();
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockFinalLessonSubmit).toHaveBeenCalledTimes(1);
      expect(mockFinalLessonSubmit).toHaveBeenCalledWith("lesson-1", "FULL");
    });
  });

  it("keeps TranslationReview open for retry on submit failure and prevents duplicate submit on rapid Enter", async () => {
    mockGetLessonProgress.mockResolvedValue({
      currentSegmentId: "s1",
      draftSegments: [
        { segmentId: "s2", userAnswer: "Sentence two.", hintCount: 0, replayCount: 0, correct: true },
        { segmentId: "s3", userAnswer: "Sentence three.", hintCount: 0, replayCount: 0, correct: true },
      ],
    });
    mockFinalLessonSubmit.mockRejectedValueOnce(new Error("Network error"));

    render(<PracticePage />);

    await waitFor(() => {
      expect(screen.getByText("Sentence 1/3")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /complete current/i }));
    expect(screen.getByRole("button", { name: /submit lesson/i })).toBeInTheDocument();

    // Rapid Enter presses should only trigger 1 submission attempt
    fireEvent.keyDown(window, { key: "Enter" });
    fireEvent.keyDown(window, { key: "Enter" });

    await waitFor(() => {
      expect(mockFinalLessonSubmit).toHaveBeenCalledTimes(1);
      expect(
        screen.getByText(/failed to submit lesson/i)
      ).toBeInTheDocument();
    });

    // TranslationReview stays visible so user can retry
    expect(screen.getByRole("button", { name: /submit lesson/i })).toBeInTheDocument();

    mockFinalLessonSubmit.mockResolvedValueOnce({ score: 100, passed: true });
    fireEvent.click(screen.getByRole("button", { name: /submit lesson/i }));

    await waitFor(() => {
      expect(mockFinalLessonSubmit).toHaveBeenCalledTimes(2);
      expect(mockPush).toHaveBeenCalledWith("/result");
    });
  });

  it("renders mobile counter badge, desktop shortcut strip, and preserves input focus across media control clicks", async () => {
    render(<PracticePage />);

    await waitFor(() => {
      expect(screen.getByText("Sentence 1/3")).toBeInTheDocument();
    });

    // Mobile counter badge & desktop shortcut strip (FULL mode shows "Continue")
    expect(screen.getByText("1/3")).toBeInTheDocument();
    expect(screen.getByText("Continue")).toBeInTheDocument();

    const textarea = screen.getByLabelText("Dictation input") as HTMLTextAreaElement;
    textarea.focus();
    expect(document.activeElement).toBe(textarea);

    const replayBtn = screen.getByRole("button", { name: /replay audio/i });
    const playBtn = screen.getByRole("button", { name: /play audio/i });
    const speedBtn = screen.getByRole("button", { name: /playback speed/i });

    // pointerdown prevents default and captures focused input; click restores focus if blurred
    const pointerDownEvent = new Event("pointerdown", { bubbles: true, cancelable: true });
    replayBtn.dispatchEvent(pointerDownEvent);
    expect(pointerDownEvent.defaultPrevented).toBe(true);

    // Simulate mobile browser blurring input anyway before click fires
    textarea.blur();
    expect(document.activeElement).not.toBe(textarea);

    fireEvent.click(replayBtn);
    expect(document.activeElement).toBe(textarea);

    // Play/Pause and Speed also preserve focus after pointerdown
    fireEvent.pointerDown(playBtn);
    textarea.blur();
    fireEvent.click(playBtn);
    expect(document.activeElement).toBe(textarea);

    fireEvent.pointerDown(speedBtn);
    textarea.blur();
    fireEvent.click(speedBtn);
    expect(document.activeElement).toBe(textarea);

    // Keyboard-activated click without pointerDown (or after pointerCancel) must NOT yank focus back
    replayBtn.focus();
    expect(document.activeElement).toBe(replayBtn);
    fireEvent.click(replayBtn);
    expect(document.activeElement).toBe(replayBtn);
  });
});
