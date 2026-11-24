import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import TranslationReview from "../TranslationReview";

describe("TranslationReview", () => {
  it("renders Submit Lesson button when isAllCompleted is true even if not on the last segment", () => {
    const onSubmitLesson = vi.fn();
    const onContinue = vi.fn();

    render(
      <TranslationReview
        sentence="Hello world."
        translatedText="Xin chào thế giới."
        isLastSegment={false}
        isAllCompleted={true}
        uncompletedCount={0}
        onContinue={onContinue}
        onSubmitLesson={onSubmitLesson}
      />
    );

    const submitBtn = screen.getByRole("button", { name: /submit lesson/i });
    expect(submitBtn).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /continue/i })
    ).not.toBeInTheDocument();

    fireEvent.click(submitBtn);
    expect(onSubmitLesson).toHaveBeenCalledTimes(1);
    expect(onContinue).not.toHaveBeenCalled();
  });

  it("renders Review Skipped without Submit Lesson when on last segment with uncompleted sentences", () => {
    const onReviewSkipped = vi.fn();
    const onSubmitLesson = vi.fn();

    render(
      <TranslationReview
        sentence="Final sentence."
        translatedText="Câu cuối."
        isLastSegment={true}
        isAllCompleted={false}
        uncompletedCount={2}
        onContinue={vi.fn()}
        onSubmitLesson={onSubmitLesson}
        onReviewSkipped={onReviewSkipped}
      />
    );

    expect(
      screen.queryByRole("button", { name: /submit lesson/i })
    ).not.toBeInTheDocument();

    const reviewBtn = screen.getByRole("button", {
      name: /review skipped \(2\)/i,
    });
    expect(reviewBtn).toBeInTheDocument();
    expect(
      screen.getByText(/complete all remaining sentences to submit/i)
    ).toBeInTheDocument();

    fireEvent.click(reviewBtn);
    expect(onReviewSkipped).toHaveBeenCalledTimes(1);
    expect(onSubmitLesson).not.toHaveBeenCalled();
  });

  it("renders Continue button when not on last segment and not all completed", () => {
    const onContinue = vi.fn();

    render(
      <TranslationReview
        sentence="First sentence."
        translatedText="Câu đầu."
        isLastSegment={false}
        isAllCompleted={false}
        uncompletedCount={3}
        onContinue={onContinue}
      />
    );

    const continueBtn = screen.getByRole("button", { name: /continue/i });
    expect(continueBtn).toBeInTheDocument();
    fireEvent.click(continueBtn);
    expect(onContinue).toHaveBeenCalledTimes(1);
  });
});
