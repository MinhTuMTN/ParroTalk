"use client";

import { useEffect } from "react";
import {
  applyThemePreference,
  useUserSettings,
} from "../hooks/useUserSettings";

export function ThemeSync() {
  const { settings } = useUserSettings();

  useEffect(() => {
    applyThemePreference(settings.theme);

    if (typeof document !== "undefined") {
      document.documentElement.lang = settings.uiLanguage;
    }

    if (
      settings.theme !== "system" ||
      typeof window === "undefined" ||
      typeof window.matchMedia !== "function"
    ) {
      return;
    }

    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    const handleMediaChange = () => {
      applyThemePreference("system");
    };

    mediaQuery.addEventListener("change", handleMediaChange);
    return () => {
      mediaQuery.removeEventListener("change", handleMediaChange);
    };
  }, [settings.theme, settings.uiLanguage]);

  return null;
}
