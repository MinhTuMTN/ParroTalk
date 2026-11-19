import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import PracticeModeSelector from "../PracticeModeSelector";

describe("PracticeModeSelector", () => {
  it("renders all 4 difficulty levels with descriptions", () => {
    const onChange = vi.fn();
    render(<PracticeModeSelector mode="FULL" onChange={onChange} />);

    expect(screen.getByRole("radio", { name: /Easy/i })).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: /Medium/i })).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: /Hard/i })).toBeInTheDocument();
    expect(
      screen.getByRole("radio", { name: /Full Dictation/i })
    ).toBeInTheDocument();

    expect(screen.getByText(/15–20% key words/i)).toBeInTheDocument();
    expect(screen.getByText(/30–40% balanced listening/i)).toBeInTheDocument();
    expect(
      screen.getByText(/50–60% connected speech & weak forms/i)
    ).toBeInTheDocument();
    expect(screen.getAllByText(/100% full sentence/i).length).toBeGreaterThanOrEqual(1);
  });

  it("marks active mode and triggers onChange when selecting another level", () => {
    const onChange = vi.fn();
    render(<PracticeModeSelector mode="MEDIUM" onChange={onChange} />);

    const mediumBtn = screen.getByRole("radio", { name: /Medium/i });
    const easyBtn = screen.getByRole("radio", { name: /Easy/i });

    expect(mediumBtn).toHaveAttribute("aria-checked", "true");
    expect(easyBtn).toHaveAttribute("aria-checked", "false");

    fireEvent.click(easyBtn);
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith("EASY");

    // Clicking already-active mode should not trigger unnecessary change
    fireEvent.click(mediumBtn);
    expect(onChange).toHaveBeenCalledTimes(1);
  });
});
