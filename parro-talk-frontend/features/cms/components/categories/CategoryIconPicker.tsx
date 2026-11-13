"use client";

import React from "react";
import {
  BookOpen,
  Briefcase,
  Coffee,
  Cpu,
  Film,
  FlaskConical,
  Folder,
  Globe,
  GraduationCap,
  Headphones,
  Heart,
  HeartPulse,
  Home,
  Leaf,
  MessageCircle,
  Music,
  Newspaper,
  Plane,
  ShoppingBag,
  Sparkles,
  TrendingUp,
  Trophy,
  Users,
  Utensils,
  type LucideIcon,
} from "lucide-react";

export interface CuratedCategoryIcon {
  name: string;
  label: string;
  Icon: LucideIcon;
}

export const CURATED_CATEGORY_ICONS: CuratedCategoryIcon[] = [
  { name: "Briefcase", label: "Work & Career", Icon: Briefcase },
  { name: "Plane", label: "Travel", Icon: Plane },
  { name: "Coffee", label: "Daily Life", Icon: Coffee },
  { name: "Heart", label: "Family & Relationships", Icon: Heart },
  { name: "HeartPulse", label: "Health", Icon: HeartPulse },
  { name: "GraduationCap", label: "Education", Icon: GraduationCap },
  { name: "Cpu", label: "Technology", Icon: Cpu },
  { name: "TrendingUp", label: "Business & Finance", Icon: TrendingUp },
  { name: "FlaskConical", label: "Science", Icon: FlaskConical },
  { name: "Leaf", label: "Environment", Icon: Leaf },
  { name: "Globe", label: "Culture & World", Icon: Globe },
  { name: "Film", label: "Entertainment", Icon: Film },
  { name: "Trophy", label: "Sports", Icon: Trophy },
  { name: "Utensils", label: "Food & Dining", Icon: Utensils },
  { name: "ShoppingBag", label: "Shopping", Icon: ShoppingBag },
  { name: "BookOpen", label: "Reading & Stories", Icon: BookOpen },
  { name: "Headphones", label: "Listening", Icon: Headphones },
  { name: "MessageCircle", label: "Conversation", Icon: MessageCircle },
  { name: "Music", label: "Music & Audio", Icon: Music },
  { name: "Sparkles", label: "Featured", Icon: Sparkles },
  { name: "Users", label: "Community", Icon: Users },
  { name: "Home", label: "Home & Living", Icon: Home },
  { name: "Newspaper", label: "News", Icon: Newspaper },
  { name: "Folder", label: "General", Icon: Folder },
];

const ICON_MAP: Record<string, LucideIcon> = Object.fromEntries(
  CURATED_CATEGORY_ICONS.map((entry) => [entry.name.toLowerCase(), entry.Icon])
);

export const PRESET_CATEGORY_COLORS: string[] = [
  "#059669", // Emerald
  "#0d9488", // Teal
  "#2563eb", // Blue
  "#4f46e5", // Indigo
  "#7c3aed", // Violet
  "#db2777", // Pink
  "#e11d48", // Rose
  "#ea580c", // Orange
  "#d97706", // Amber
  "#475569", // Slate
];

export function renderCategoryIcon(
  iconName: string | null | undefined,
  className = "h-4 w-4"
) {
  const normalized = iconName?.trim().toLowerCase() ?? "";
  const Icon = ICON_MAP[normalized] ?? Folder;
  return <Icon className={className} aria-hidden="true" />;
}

interface CategoryIconPickerProps {
  value: string;
  accentColor: string;
  onChange: (iconName: string) => void;
}

export default function CategoryIconPicker({
  value,
  accentColor,
  onChange,
}: CategoryIconPickerProps) {
  const normalizedSelected = value.trim().toLowerCase();

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium text-slate-500">
          Pick a Lucide icon ({CURATED_CATEGORY_ICONS.length} presets)
        </span>
        {value ? (
          <button
            type="button"
            onClick={() => onChange("")}
            className="text-xs font-semibold text-slate-500 hover:text-slate-800 transition-colors cursor-pointer"
          >
            Clear
          </button>
        ) : null}
      </div>

      <div
        role="group"
        aria-label="Category icon options"
        className="grid grid-cols-6 sm:grid-cols-8 gap-1.5 rounded-2xl border border-slate-200 bg-slate-50/70 p-2.5"
      >
        {CURATED_CATEGORY_ICONS.map(({ name, label, Icon }) => {
          const isSelected = normalizedSelected === name.toLowerCase();
          return (
            <button
              key={name}
              type="button"
              title={`${label} (${name})`}
              aria-label={`Select ${label} icon`}
              aria-pressed={isSelected}
              onClick={() => onChange(name)}
              style={
                isSelected
                  ? {
                      borderColor: accentColor,
                      backgroundColor: `${accentColor}18`,
                      color: accentColor,
                    }
                  : undefined
              }
              className={`flex h-9 w-full items-center justify-center rounded-xl border text-slate-600 transition-all cursor-pointer outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 ${
                isSelected
                  ? "shadow-xs font-semibold"
                  : "border-transparent bg-white hover:border-slate-200 hover:text-slate-900"
              }`}
            >
              <Icon className="h-4 w-4" aria-hidden="true" />
            </button>
          );
        })}
      </div>
    </div>
  );
}
