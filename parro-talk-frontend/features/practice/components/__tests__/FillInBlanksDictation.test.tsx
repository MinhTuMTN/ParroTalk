import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import FillInBlanksDictation from "../FillInBlanksDictation";
import type { DraftSegmentResponse } from "@/features/lesson/services/lessonService";

describe("FillInBlanksDictation", () => {
  it("renders exact blank count with uniform width classes and no character length leaks", () => {
    const sentence = "I extraordinary went to hippopotamus yesterday!";
    // wordIndex 0="I" (len 1), 1="extraordinary" (len 13), 4="hippopotamus" (len 12)
    render(
      <FillInBlanksDictation
        sentence={sentence}
        mode="MEDIUM"
        blanks={[{ wordIndex: 0 }, { wordIndex: 1 }, { wordIndex: 4 }]}
        suitableForBlanks
        onSubmitBlanks={vi.fn()}
        onSegmentComplete={vi.fn()}
      />
    );

    const inputs = screen.getAllByRole("textbox");
    expect(inputs).toHaveLength(3);

    for (const input of inputs) {
      expect(input).not.toHaveAttribute("size");
      expect(input).not.toHaveAttribute("placeholder");
      expect(input.className).toContain("w-[6.75rem]");
      expect(input.className).toContain("sm:w-[7.5rem]");
    }

    // Preserves visible words and punctuation outside inputs
    expect(screen.getByText("went")).toBeInTheDocument();
    expect(screen.getByText("yesterday")).toBeInTheDocument();
    expect(screen.getByText("!")).toBeInTheDocument();
  });

  it("treats repeated words as independent blank inputs", () => {
    const sentence = "Go, go, go now!";
    render(
      <FillInBlanksDictation
        sentence={sentence}
        mode="EASY"
        blanks={[{ wordIndex: 0 }, { wordIndex: 2 }]}
        suitableForBlanks
        onSubmitBlanks={vi.fn()}
        onSegmentComplete={vi.fn()}
      />
    );

    const inputs = screen.getAllByRole("textbox");
    expect(inputs).toHaveLength(2);

    fireEvent.change(inputs[0], { target: { value: "first" } });
    fireEvent.change(inputs[1], { target: { value: "second" } });

    expect(inputs[0]).toHaveValue("first");
    expect(inputs[1]).toHaveValue("second");
  });

  it("handles per-blank correct/incorrect feedback, retry edit clearing error, and completion", async () => {
    const onSegmentComplete = vi.fn();
    const onSubmitBlanks = vi
      .fn()
      .mockResolvedValueOnce({
        segmentId: "seg-1",
        userAnswer: "don’t | wrng",
        score: 0,
        replayCount: 0,
        hintCount: 0,
        correct: false,
        blankResults: [
          { wordIndex: 1, correct: true, expectedAnswer: "don’t" },
          { wordIndex: 3, correct: false, expectedAnswer: null },
        ],
        accuracy: 0.5,
      } satisfies DraftSegmentResponse)
      .mockResolvedValueOnce({
        segmentId: "seg-1",
        userAnswer: "don’t | later",
        score: 100,
        replayCount: 0,
        hintCount: 0,
        correct: true,
        blankResults: [
          { wordIndex: 1, correct: true, expectedAnswer: "don’t" },
          { wordIndex: 3, correct: true, expectedAnswer: "later" },
        ],
        accuracy: 1,
      } satisfies DraftSegmentResponse);

    render(
      <FillInBlanksDictation
        sentence="I don’t leave later."
        mode="MEDIUM"
        blanks={[{ wordIndex: 1 }, { wordIndex: 3 }]}
        suitableForBlanks
        onSubmitBlanks={onSubmitBlanks}
        onSegmentComplete={onSegmentComplete}
      />
    );

    const inputs = screen.getAllByRole("textbox");
    fireEvent.change(inputs[0], { target: { value: "don't" } });
    fireEvent.change(inputs[1], { target: { value: "wrng" } });

    fireEvent.click(screen.getByRole("button", { name: /check/i }));

    await waitFor(() => {
      expect(screen.getByText("1 / 2 blanks correct")).toBeInTheDocument();
    });

    expect(inputs[0]).toHaveAttribute("readonly");
    expect(inputs[0].className).toContain("border-brand-500");
    expect(inputs[1]).not.toHaveAttribute("readonly");
    expect(inputs[1]).toHaveAttribute("aria-invalid", "true");
    expect(inputs[1].className).toContain("border-coral-500");
    expect(onSegmentComplete).not.toHaveBeenCalled();

    // Editing incorrect blank clears error state
    fireEvent.change(inputs[1], { target: { value: "later" } });
    expect(inputs[1]).toHaveAttribute("aria-invalid", "false");

    fireEvent.click(screen.getByRole("button", { name: /check/i }));

    await waitFor(() => {
      expect(onSegmentComplete).toHaveBeenCalledTimes(1);
    });
  });

  it("supports keyboard navigation (Enter advances, Enter on last triggers check, Backspace returns) and Hint button", async () => {
    const onHintUsed = vi.fn();
    const onSubmitBlanks = vi.fn().mockResolvedValue({
      segmentId: "seg-2",
      userAnswer: "hello | world",
      score: 100,
      replayCount: 0,
      hintCount: 1,
      correct: true,
      blankResults: [
        { wordIndex: 0, correct: true, expectedAnswer: "Hello" },
        { wordIndex: 1, correct: true, expectedAnswer: "world" },
      ],
    } satisfies DraftSegmentResponse);

    render(
      <FillInBlanksDictation
        sentence="Hello world today."
        mode="EASY"
        blanks={[{ wordIndex: 0 }, { wordIndex: 1 }]}
        suitableForBlanks
        onSubmitBlanks={onSubmitBlanks}
        onSegmentComplete={vi.fn()}
        onHintUsed={onHintUsed}
      />
    );

    const [first, second] = screen.getAllByRole("textbox");
    first.focus();
    fireEvent.keyDown(first, { key: "Enter" });
    expect(document.activeElement).toBe(second);

    // Backspace on empty second blank focuses first blank
    fireEvent.keyDown(second, { key: "Backspace" });
    expect(document.activeElement).toBe(first);

    // Hint reveals focused blank and charges hint
    fireEvent.click(screen.getByRole("button", { name: /hint/i }));
    expect(first).toHaveValue("Hello");
    expect(first).toHaveAttribute("readonly");
    expect(onHintUsed).toHaveBeenCalledTimes(1);

    // Enter on last blank triggers check
    fireEvent.change(second, { target: { value: "world" } });
    fireEvent.keyDown(second, { key: "Enter" });

    await waitFor(() => {
      expect(onSubmitBlanks).toHaveBeenCalledTimes(1);
    });
  });

  it("renders clean fallback experience when segment is unsuitable for blanks", () => {
    render(
      <FillInBlanksDictation
        sentence="Okay."
        mode="EASY"
        blanks={[]}
        suitableForBlanks={false}
        fallbackInput=""
        onFallbackInputChange={vi.fn()}
        onSubmitBlanks={vi.fn()}
        onSegmentComplete={vi.fn()}
      />
    );

    expect(screen.getByTestId("fill-blanks-fallback")).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("Type what you hear...")
    ).toBeInTheDocument();
  });
});
