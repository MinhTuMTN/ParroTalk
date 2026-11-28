import { afterEach, describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen } from "@testing-library/react";
import FillInBlanksDictation from "../FillInBlanksDictation";

describe("FillInBlanksDictation", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("scales blank width with word length, renders mask overlay bullets, sets default aria-invalid='false', and omits bottom Hint/Check buttons", () => {
    const sentence = "I extraordinary went to hippopotamus yesterday!";
    // wordIndex 0="I" (len 1), 1="extraordinary" (len 13), 4="hippopotamus" (len 12)
    const { container } = render(
      <FillInBlanksDictation
        sentence={sentence}
        mode="MEDIUM"
        blanks={[{ wordIndex: 0 }, { wordIndex: 1 }, { wordIndex: 4 }]}
        suitableForBlanks
        onSegmentComplete={vi.fn()}
      />
    );

    const inputs = screen.getAllByRole("textbox");
    expect(inputs).toHaveLength(3);

    for (const input of inputs) {
      expect(input).not.toHaveAttribute("size");
      expect(input).not.toHaveAttribute("maxlength");
      expect(input).not.toHaveAttribute("placeholder");
      expect(input).toHaveAttribute("aria-invalid", "false");
      expect(input).toHaveAttribute("inputmode", "text");
      expect(input.className).not.toContain("w-[5.25rem]");
      expect(input.className).not.toContain("sm:w-[6rem]");
      expect(input.className).toContain("max-w-full");
      expect(input.className).toContain("font-mono");
      expect(input.className).toContain("bg-transparent");
      expect(input.className).toContain("text-base");
      expect(input.className).toContain("h-9");
      expect(input.className).toContain("text-left");
    }

    // Width scales with word length (min 2ch)
    expect(inputs[0].style.width).toContain("2ch");
    expect(inputs[1].style.width).toContain("13ch");
    expect(inputs[2].style.width).toContain("12ch");

    // Mask overlay shows exact count of bullets matching alphanumeric characters
    const overlays = container.querySelectorAll('[aria-hidden="true"].font-mono');
    expect(overlays).toHaveLength(3);
    expect(overlays[0].textContent).toBe("•");
    expect(overlays[1].textContent).toBe("•".repeat(13));
    expect(overlays[2].textContent).toBe("•".repeat(12));

    // Typing shrinks the visible bullet tail while preserving typed prefix in invisible span
    fireEvent.change(inputs[1], { target: { value: "ext" } });
    expect(overlays[1].querySelector(".invisible")?.textContent).toBe("ext");
    expect(overlays[1].textContent).toBe(`ext${"•".repeat(10)}`);

    expect(inputs[0]).toHaveAttribute("enterkeyhint", "next");
    expect(inputs[1]).toHaveAttribute("enterkeyhint", "next");
    expect(inputs[2]).toHaveAttribute("enterkeyhint", "done");

    // Bottom Hint and Check buttons are removed
    expect(screen.queryByRole("button", { name: /^hint$/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /^check$/i })).not.toBeInTheDocument();

    // Inline hint buttons exist with tabIndex=-1
    const inlineHints = screen.getAllByRole("button", { name: /reveal hint/i });
    expect(inlineHints).toHaveLength(3);
    for (const btn of inlineHints) {
      expect(btn).toHaveAttribute("tabindex", "-1");
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

  it("auto-completes after 40ms when all blanks are valid and auto-jumps focus on completing a non-last blank", () => {
    vi.useFakeTimers();
    const onSegmentComplete = vi.fn();

    render(
      <FillInBlanksDictation
        sentence="I don’t leave later."
        mode="MEDIUM"
        blanks={[{ wordIndex: 1 }, { wordIndex: 3 }]}
        suitableForBlanks
        onSegmentComplete={onSegmentComplete}
      />
    );

    const inputs = screen.getAllByRole("textbox");
    inputs[0].focus();
    fireEvent.change(inputs[0], { target: { value: "don't" } });
    // D3 auto-jump moves focus to next editable blank without locking the first blank
    expect(document.activeElement).toBe(inputs[1]);
    expect(inputs[0]).not.toHaveAttribute("readonly");
    expect(onSegmentComplete).not.toHaveBeenCalled();

    fireEvent.change(inputs[1], { target: { value: "later" } });

    // Green glow applied immediately, callback fires after 40ms
    expect(inputs[0].className).toContain("border-brand-500");
    expect(inputs[1].className).toContain("border-brand-500");
    expect(onSegmentComplete).not.toHaveBeenCalled();

    act(() => {
      vi.advanceTimersByTime(40);
    });

    expect(onSegmentComplete).toHaveBeenCalledTimes(1);
    expect(onSegmentComplete).toHaveBeenCalledWith({
      userAnswer: "I don't leave later.",
      blankAnswers: [
        { wordIndex: 1, answer: "don't" },
        { wordIndex: 3, answer: "later" },
      ],
    });
  });

  it("reveals blank via inline ? hint button or Alt+H and auto-completes when last blank becomes valid", () => {
    vi.useFakeTimers();
    const onHintUsed = vi.fn();
    const onSegmentComplete = vi.fn();

    render(
      <FillInBlanksDictation
        sentence="Hello world today."
        mode="EASY"
        blanks={[{ wordIndex: 0 }, { wordIndex: 1 }]}
        suitableForBlanks
        onSegmentComplete={onSegmentComplete}
        onHintUsed={onHintUsed}
      />
    );

    const [first, second] = screen.getAllByRole("textbox");
    const hintButtons = screen.getAllByRole("button", { name: /reveal hint/i });

    // Click inline ? button on first blank
    fireEvent.click(hintButtons[0]);
    expect(first).toHaveValue("Hello");
    expect(first).toHaveAttribute("readonly");
    expect(onHintUsed).toHaveBeenCalledTimes(1);
    expect(document.activeElement).toBe(second);

    // Reveal second blank via Option+H / Alt+H shortcut (including macOS e.code === "KeyH") -> triggers auto-complete after 40ms
    fireEvent.keyDown(second, { key: "˙", code: "KeyH", altKey: true });
    expect(second).toHaveValue("world");
    expect(second).toHaveAttribute("readonly");
    expect(onHintUsed).toHaveBeenCalledTimes(2);

    act(() => {
      vi.advanceTimersByTime(40);
    });

    expect(onSegmentComplete).toHaveBeenCalledTimes(1);
    expect(onSegmentComplete).toHaveBeenCalledWith({
      userAnswer: "Hello world today.",
      blankAnswers: [
        { wordIndex: 0, answer: "Hello" },
        { wordIndex: 1, answer: "world" },
      ],
    });
  });

  it("handles Enter navigation, commit-time invalid marking, live region feedback, and focus on first invalid blank", () => {
    const onSegmentComplete = vi.fn();

    render(
      <FillInBlanksDictation
        sentence="I don’t leave later."
        mode="MEDIUM"
        blanks={[{ wordIndex: 1 }, { wordIndex: 3 }]}
        suitableForBlanks
        onSegmentComplete={onSegmentComplete}
      />
    );

    const [first, second] = screen.getAllByRole("textbox");

    // Enter on non-last blank moves focus to next editable blank
    first.focus();
    fireEvent.change(first, { target: { value: "wrng1" } });
    expect(first).toHaveAttribute("aria-invalid", "true");
    expect(first.className).not.toContain("border-coral-500");
    fireEvent.keyDown(first, { key: "Enter" });
    expect(document.activeElement).toBe(second);

    // Backspace on empty second blank focuses first blank
    fireEvent.keyDown(second, { key: "Backspace" });
    expect(document.activeElement).toBe(first);

    // Navigate back to second blank, type wrong answer, and commit with Enter
    fireEvent.keyDown(first, { key: "Enter" });
    fireEvent.change(second, { target: { value: "wrng2" } });
    fireEvent.keyDown(second, { key: "Enter" });

    expect(onSegmentComplete).not.toHaveBeenCalled();
    expect(first).toHaveAttribute("aria-invalid", "true");
    expect(second).toHaveAttribute("aria-invalid", "true");
    expect(first.className).toContain("border-coral-500");
    expect(second.className).toContain("border-coral-500");
    expect(document.activeElement).toBe(first);
    expect(
      screen.getByText(/2 blanks are incorrect/i)
    ).toBeInTheDocument();

    // Editing first blank immediately clears its error state
    fireEvent.change(first, { target: { value: "don't" } });
    expect(first).toHaveAttribute("aria-invalid", "false");
  });

  it("displays expected words in read-only inputs when isCompleted is true without draftState", () => {
    render(
      <FillInBlanksDictation
        sentence="Hello world today."
        mode="EASY"
        blanks={[{ wordIndex: 0 }, { wordIndex: 1 }]}
        suitableForBlanks
        isCompleted
        onSegmentComplete={vi.fn()}
      />
    );

    const [first, second] = screen.getAllByRole("textbox");
    expect(first).toHaveValue("Hello");
    expect(second).toHaveValue("world");
    expect(first).toHaveAttribute("readonly");
    expect(second).toHaveAttribute("readonly");
    expect(first.className).toContain("text-center");
    expect(second.className).toContain("text-center");
  });

  it("updates enterKeyHint to 'done' on the remaining editable blank when the last blank is locked via hint", () => {
    render(
      <FillInBlanksDictation
        sentence="Hello world today."
        mode="EASY"
        blanks={[{ wordIndex: 0 }, { wordIndex: 1 }]}
        suitableForBlanks
        onSegmentComplete={vi.fn()}
      />
    );

    const [first, second] = screen.getAllByRole("textbox");
    expect(first).toHaveAttribute("enterkeyhint", "next");
    expect(second).toHaveAttribute("enterkeyhint", "done");

    const hintButtons = screen.getAllByRole("button", { name: /reveal hint/i });
    fireEvent.click(hintButtons[1]);

    expect(second).toHaveAttribute("readonly");
    expect(second.className).toContain("text-center");
    expect(first).toHaveAttribute("enterkeyhint", "done");
    expect(first.className).toContain("text-left");
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
        onSegmentComplete={vi.fn()}
      />
    );

    expect(screen.getByTestId("fill-blanks-fallback")).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("Type what you hear...")
    ).toBeInTheDocument();
  });

  it("marks wrong characters with aria-invalid='true' and red styling immediately without Enter", () => {
    render(
      <FillInBlanksDictation
        sentence="Work hard today."
        mode="EASY"
        blanks={[{ wordIndex: 0 }, { wordIndex: 2 }]}
        suitableForBlanks
        onSegmentComplete={vi.fn()}
      />
    );

    const [first] = screen.getAllByRole("textbox");
    fireEvent.change(first, { target: { value: "wo" } });
    expect(first).toHaveAttribute("aria-invalid", "false");
    expect(first.className).not.toContain("border-red-400");

    fireEvent.change(first, { target: { value: "wox" } });
    expect(first).toHaveAttribute("aria-invalid", "true");
    expect(first.className).toContain("border-red-400");
    expect(first.className).toContain("text-red-600");
  });

  it("moves focus to the next editable blank when a word becomes correct without locking it", () => {
    render(
      <FillInBlanksDictation
        sentence="I work hard today."
        mode="MEDIUM"
        blanks={[{ wordIndex: 1 }, { wordIndex: 3 }]}
        suitableForBlanks
        onSegmentComplete={vi.fn()}
      />
    );

    const [first, second] = screen.getAllByRole("textbox");
    first.focus();
    fireEvent.change(first, { target: { value: "wor" } });
    expect(document.activeElement).toBe(first);

    fireEvent.change(first, { target: { value: "work" } });
    expect(document.activeElement).toBe(second);
    expect(first).not.toHaveAttribute("readonly");
    expect(first.className).toContain("border-brand-500");
  });

  it("treats 'dont' for \"don't\" as valid green and not red", () => {
    render(
      <FillInBlanksDictation
        sentence="I don’t leave later."
        mode="MEDIUM"
        blanks={[{ wordIndex: 1 }, { wordIndex: 3 }]}
        suitableForBlanks
        onSegmentComplete={vi.fn()}
      />
    );

    const [first] = screen.getAllByRole("textbox");
    fireEvent.change(first, { target: { value: "dont" } });
    expect(first).toHaveAttribute("aria-invalid", "false");
    expect(first.className).not.toContain("border-red-400");
    expect(first.className).not.toContain("border-coral-500");
    expect(first.className).toContain("border-brand-500");
  });

  it("suppresses wrong styling and auto-jump during IME composition and auto-jumps on compositionEnd when correct", () => {
    render(
      <FillInBlanksDictation
        sentence="I work hard today."
        mode="MEDIUM"
        blanks={[{ wordIndex: 1 }, { wordIndex: 3 }]}
        suitableForBlanks
        onSegmentComplete={vi.fn()}
      />
    );

    const [first, second] = screen.getAllByRole("textbox");
    first.focus();

    fireEvent.compositionStart(first);
    fireEvent.change(first, { target: { value: "wox" } });
    expect(first).toHaveAttribute("aria-invalid", "false");
    expect(first.className).not.toContain("border-red-400");
    expect(document.activeElement).toBe(first);

    fireEvent.change(first, { target: { value: "work" } });
    expect(document.activeElement).toBe(first);

    fireEvent.compositionEnd(first, { target: { value: "work" } });
    expect(document.activeElement).toBe(second);
    expect(first).not.toHaveAttribute("readonly");
    expect(first.className).toContain("border-brand-500");
  });

  it("hides remaining mask bullets when a shorter valid equivalent is entered", () => {
    const { container } = render(
      <FillInBlanksDictation
        sentence="Wait twenty-one minutes today."
        mode="MEDIUM"
        blanks={[{ wordIndex: 1 }, { wordIndex: 3 }]}
        suitableForBlanks
        onSegmentComplete={vi.fn()}
      />
    );

    const [first] = screen.getAllByRole("textbox");
    const overlays = container.querySelectorAll('[aria-hidden="true"].font-mono');
    expect(overlays[0].textContent).toBe("••••••-•••");

    fireEvent.change(first, { target: { value: "21" } });
    expect(overlays[0].textContent).toBe("21");
    expect(overlays[0].textContent).not.toContain("•");
  });
});
