/**
 * ParroTalk Unified Color Palette & Design Tokens
 * Used across the entire application for consistent styling, Canvas animations,
 * metadata theme colors, charts, and inline styles.
 */

export const COLORS = {
  // Primary Brand: Emerald (Energizing, fresh, inspiring learning)
  primary: {
    50: "#ecfdf5",
    100: "#d1fae5",
    200: "#a7f3d0",
    300: "#6ee7b7",
    400: "#34d399",
    500: "#10b981",
    600: "#059669", // Primary Default
    700: "#047857",
    800: "#065f46",
    900: "#064e3b",
    950: "#022c22",
  },

  // Accent / CTA: Amber (Action-oriented, highlights critical buttons)
  accent: {
    50: "#fffbeb",
    100: "#fef3c7",
    200: "#fde68a",
    300: "#fcd34d",
    400: "#fbbf24",
    500: "#f59e0b", // Accent Default (CTA button, hot badge)
    600: "#d97706", // Accent Border (3D button border)
    700: "#b45309",
    800: "#92400e",
    900: "#78350f",
  },

  // Neutral / Backgrounds
  background: {
    page: "#f8fafc",      // Slate 50 - Standard page background
    surface: "#ffffff",   // Pure White - Cards, modals, header
    soft: "#fdfdfd",      // Soft White - Auth & dashboard backdrop
    subtle: "#f1f5f9",    // Slate 100 - Input background & soft borders
    dark: "#0f172a",      // Slate 900 - Footer & dark accents
  },

  // Typography Colors
  text: {
    primary: "#0f172a",   // Slate 900 - Headings & primary body
    secondary: "#475569", // Slate 600 - Secondary text
    muted: "#94a3b8",     // Slate 400 - Muted captions & placeholders
    inverse: "#ffffff",   // Pure White - Inverted text
  },

  // Status & Feedback Colors
  status: {
    success: "#10b981",
    warning: "#f59e0b",
    error: "#ef4444",
    info: "#3b82f6",
  },

  // Confetti / Particle Effects
  confetti: ["#059669", "#10b981", "#34d399", "#f59e0b", "#fbbf24"],
} as const;

export const THEME_COLOR = COLORS.primary[600]; // #059669
export const APP_BACKGROUND_COLOR = COLORS.background.page; // #f8fafc
