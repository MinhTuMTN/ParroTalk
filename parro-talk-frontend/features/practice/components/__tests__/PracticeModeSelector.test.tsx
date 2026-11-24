import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import PracticeModeSelector from "../PracticeModeSelector";

describe("PracticeModeSelector", () => {
  it("renders compact trigger pill closed by default and opens all 4 radio options on click", () => {
    const onChange = vi.fn();
    render(<PracticeModeSelector mode="FULL" onChange={onChange} />);

    const trigger = screen.getByRole("button", {
      name: /Dictation mode: Full Dictation \(100%\)/i,
    });
    expect(trigger).toHaveAttribute("aria-haspopup", "dialog");
    expect(trigger).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByRole("radiogroup")).not.toBeInTheDocument();

    fireEvent.click(trigger);

    expect(trigger).toHaveAttribute("aria-expanded", "true");
    expect(
      screen.getByRole("radiogroup", { name: /Dictation difficulty mode/i })
    ).toBeInTheDocument();

    expect(screen.getByRole("radio", { name: /Easy/i })).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: /Medium/i })).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: /Hard/i })).toBeInTheDocument();
    expect(
      screen.getByRole("radio", { name: /Full Dictation/i })
    ).toBeInTheDocument();

    expect(screen.getByText("~15–20% key words")).toBeInTheDocument();
    expect(screen.getByText("~30–40% balanced listening")).toBeInTheDocument();
    expect(
      screen.getByText("~50–60% connected speech & weak forms")
    ).toBeInTheDocument();
    expect(screen.getByText("100% full sentence")).toBeInTheDocument();
  });

  it("calls onChange and closes dropdown when selecting another mode, and closes without calling onChange for active mode", () => {
    const onChange = vi.fn();
    render(<PracticeModeSelector mode="MEDIUM" onChange={onChange} />);

    const trigger = screen.getByRole("button", {
      name: /Dictation mode: Medium \(30–40%\)/i,
    });

    fireEvent.click(trigger);

    const mediumBtn = screen.getByRole("radio", { name: /Medium/i });
    const easyBtn = screen.getByRole("radio", { name: /Easy/i });

    expect(mediumBtn).toHaveAttribute("aria-checked", "true");
    expect(easyBtn).toHaveAttribute("aria-checked", "false");

    // Clicking a different option calls onChange and closes the popover
    fireEvent.click(easyBtn);
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith("EASY");
    expect(trigger).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByRole("radiogroup")).not.toBeInTheDocument();

    // Re-open and click the already-active option: closes without calling onChange
    fireEvent.click(trigger);
    fireEvent.click(screen.getByRole("radio", { name: /Medium/i }));
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(trigger).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByRole("radiogroup")).not.toBeInTheDocument();
  });

  it("dismisses on outside mousedown and capture-phase Escape key without leaking to replay handlers", () => {
    const onChange = vi.fn();
    render(
      <div>
        <PracticeModeSelector mode="HARD" onChange={onChange} />
        <button type="button">Outside target</button>
      </div>
    );

    const trigger = screen.getByRole("button", {
      name: /Dictation mode: Hard \(50–60%\)/i,
    });

    // Outside click dismissal
    fireEvent.click(trigger);
    expect(screen.getByRole("radiogroup")).toBeInTheDocument();
    fireEvent.mouseDown(screen.getByRole("button", { name: "Outside target" }));
    expect(screen.queryByRole("radiogroup")).not.toBeInTheDocument();

    // Capture-phase Escape dismissal stops propagation to bubble listeners
    const bubbleEscapeListener = vi.fn();
    window.addEventListener("keydown", bubbleEscapeListener);

    fireEvent.click(trigger);
    expect(screen.getByRole("radiogroup")).toBeInTheDocument();

    const escEvent = new KeyboardEvent("keydown", {
      key: "Escape",
      bubbles: true,
      cancelable: true,
    });
    window.dispatchEvent(escEvent);

    expect(screen.queryByRole("radiogroup")).not.toBeInTheDocument();
    expect(escEvent.defaultPrevented).toBe(true);
    expect(bubbleEscapeListener).not.toHaveBeenCalled();

    window.removeEventListener("keydown", bubbleEscapeListener);
  });

  it("disables trigger when disabled prop is true", () => {
    const onChange = vi.fn();
    render(<PracticeModeSelector mode="FULL" onChange={onChange} disabled />);

    const trigger = screen.getByRole("button", {
      name: /Dictation mode: Full Dictation \(100%\)/i,
    });
    expect(trigger).toBeDisabled();
    fireEvent.click(trigger);
    expect(screen.queryByRole("radiogroup")).not.toBeInTheDocument();
  });
});
