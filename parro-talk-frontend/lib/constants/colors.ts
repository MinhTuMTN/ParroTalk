/**
 * ParroTalk Unified Color Palette & Design Tokens
 * Sage & Mint Educational Aesthetic
 * Used across the entire application for consistent styling, Canvas animations,
 * metadata theme colors, charts, and inline styles.
 */

export const COLORS = {
  // Primary Brand: Sage & Mint (Fresh, organic, calm, inspiring learning)
  primary: {
    50: "#f0fdf4",
    100: "#dcfce7",
    200: "#bbf7d0",
    300: "#86efac",
    400: "#4eaa78",
    500: "#3b9e66", // Sage Green - Primary Default
    600: "#2d8553",
    700: "#047857",
    800: "#065f46",
    900: "#064e3b",
    950: "#022c22",
  },

  // Secondary Accent: Warm Coral / Peach (Warmth, streak, highlights)
  secondary: {
    50: "#fff7ed",
    100: "#ffedd5",
    200: "#fed7aa",
    300: "#fdba74",
    400: "#fb923c",
    500: "#ff7a59", // Coral Default
    600: "#ea580c",
    700: "#c2410c",
    800: "#9a3412",
    900: "#7c2d12",
  },

  // Accent / CTA: Amber (Action-oriented, hot badge)
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
    page: "#f8faf9",      // Soft warm tinted off-white base
    surface: "#ffffff",   // Pure White - Cards, modals, header
    soft: "#f4f8f5",      // Soft Mint Tint - Auth & dashboard backdrop
    subtle: "#eaf3ed",    // Mint Tint 100 - Input background & soft borders
    dark: "#0f172a",      // Slate 900 - Footer & dark accents
  },

  // Typography Colors
  text: {
    primary: "#111827",   // Slate 900 - Headings & primary body
    secondary: "#4b5563", // Slate 600 - Secondary text
    muted: "#9ca3af",     // Slate 400 - Muted captions & placeholders
    inverse: "#ffffff",   // Pure White - Inverted text
  },

  // Status & Feedback Colors
  status: {
    success: "#22c55e",
    warning: "#f59e0b",
    error: "#ef4444",
    info: "#3b82f6",
  },

  // Confetti / Particle Effects
  confetti: ["#3b9e66", "#4eaa78", "#86efac", "#ff7a59", "#f59e0b", "#fbbf24"],
} as const;

export const THEME_COLOR = COLORS.primary[500]; // #3b9e66
export const APP_BACKGROUND_COLOR = COLORS.background.page; // #f8faf9
