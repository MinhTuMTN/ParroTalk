import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import PracticeHeader from "../PracticeHeader";
import { DEFAULT_USER_SETTINGS } from "@/features/settings/types";

let mockSettings = { ...DEFAULT_USER_SETTINGS, uiLanguage: "en" as const };

vi.mock("@/features/auth/hooks/useAuth", () => ({
  useAuth: () => ({ user: { fullName: "Alice" } }),
}));

vi.mock("@/hooks/useUI", () => ({
  useUI: () => ({ openMobileMenu: vi.fn() }),
}));

vi.mock("@/features/settings/hooks/useUserSettings", () => ({
  useUserSettings: () => ({
    settings: mockSettings,
    updateSettings: vi.fn(),
  }),
}));

describe("PracticeHeader", () => {
  it("renders progress and does not render any top-right Submit button", () => {
    mockSettings = { ...DEFAULT_USER_SETTINGS, uiLanguage: "en" };
    render(
      <PracticeHeader
        currentSentence={4}
        totalSentences={4}
        percent={100}
        mode="FULL"
        onModeChange={vi.fn()}
      />
    );

    expect(screen.getByText("Sentence 4/4")).toBeInTheDocument();
    expect(screen.getByText("100%")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /submit|nộp bài/i })
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /quick settings/i })
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /keyboard shortcuts/i })
    ).toBeInTheDocument();
  });

  it("toggles keyboard shortcuts popover, closes quick settings, displays mode-specific shortcuts, and closes on Escape without bubbling", () => {
    mockSettings = {
      ...DEFAULT_USER_SETTINGS,
      uiLanguage: "en",
      dictationReplayKey: "Tab",
    };
    const { container } = render(
      <PracticeHeader
        currentSentence={1}
        totalSentences={4}
        percent={25}
        mode="MEDIUM"
        onModeChange={vi.fn()}
      />
    );

    const shortcutsBtn = screen.getByRole("button", {
      name: /keyboard shortcuts/i,
    });
    const quickSettingsBtn = screen.getByRole("button", {
      name: /quick settings/i,
    });

    expect(shortcutsBtn).toHaveAttribute("aria-expanded", "false");
    expect(shortcutsBtn).toHaveAttribute(
      "aria-controls",
      "practice-shortcuts-popover"
    );
    expect(
      container.querySelector('[data-shortcuts-open="true"]')
    ).not.toBeInTheDocument();

    // Open quick settings first, then click shortcuts -> closes quick settings and opens shortcuts
    fireEvent.click(quickSettingsBtn);
    expect(quickSettingsBtn).toHaveAttribute("aria-expanded", "true");

    fireEvent.click(shortcutsBtn);
    expect(quickSettingsBtn).toHaveAttribute("aria-expanded", "false");
    expect(shortcutsBtn).toHaveAttribute("aria-expanded", "true");
    expect(
      container.querySelector('[data-shortcuts-open="true"]')
    ).toBeInTheDocument();
    expect(
      screen.getByRole("region", { name: /keyboard shortcuts/i })
    ).toHaveAttribute("id", "practice-shortcuts-popover");

    // Reflects configured replay key ("Tab") and blank-mode shortcuts
    expect(screen.getByText("Replay audio")).toBeInTheDocument();
    expect(screen.getByText("Reveal blank hint")).toBeInTheDocument();
    expect(screen.getByText(/Alt \+ H/i)).toBeInTheDocument();
    expect(screen.getByText(/macOS: ⌥ \+ H/i)).toBeInTheDocument();
    expect(screen.getAllByText("Tab").length).toBeGreaterThanOrEqual(2);

    // Pressing Escape in capture phase closes popover and prevents bubble-phase listeners from firing
    const bubbleListener = vi.fn();
    window.addEventListener("keydown", bubbleListener);
    fireEvent.keyDown(window, { key: "Escape" });
    window.removeEventListener("keydown", bubbleListener);

    expect(bubbleListener).not.toHaveBeenCalled();
    expect(shortcutsBtn).toHaveAttribute("aria-expanded", "false");
    expect(
      container.querySelector('[data-shortcuts-open="true"]')
    ).not.toBeInTheDocument();
  });

  it("hides blank-specific shortcuts in FULL mode and closes on outside click", () => {
    mockSettings = {
      ...DEFAULT_USER_SETTINGS,
      uiLanguage: "en",
      dictationReplayKey: "Control",
    };
    render(
      <PracticeHeader
        currentSentence={1}
        totalSentences={4}
        percent={25}
        mode="FULL"
        onModeChange={vi.fn()}
      />
    );

    const shortcutsBtn = screen.getByRole("button", {
      name: /keyboard shortcuts/i,
    });
    fireEvent.click(shortcutsBtn);

    expect(screen.getByText("Ctrl")).toBeInTheDocument();
    expect(screen.getByText("Replay audio")).toBeInTheDocument();
    expect(screen.queryByText("Reveal blank hint")).not.toBeInTheDocument();

    fireEvent.mouseDown(document.body);
    expect(shortcutsBtn).toHaveAttribute("aria-expanded", "false");

    // Re-open and verify focusin outside closes popover (Tab-out)
    fireEvent.click(shortcutsBtn);
    expect(shortcutsBtn).toHaveAttribute("aria-expanded", "true");
    fireEvent.focusIn(document.body);
    expect(shortcutsBtn).toHaveAttribute("aria-expanded", "false");
  });
});
