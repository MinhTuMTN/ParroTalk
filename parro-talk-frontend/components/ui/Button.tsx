"use client";

import type { ButtonHTMLAttributes, ReactNode } from "react";
import { Loader2 } from "lucide-react";

export type ButtonVariant =
  | "primary"
  | "secondary"
  | "outline"
  | "ghost"
  | "danger"
  | "dark"
  | "social";

export type ButtonSize = "sm" | "default" | "md" | "lg" | "xl" | "icon" | string;

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  fullWidth?: boolean;
}

const variantClassMap: Record<ButtonVariant, string> = {
  primary:
    "bg-emerald-600 text-white hover:bg-emerald-700 shadow-md shadow-emerald-600/20 active:scale-[0.98]",
  secondary:
    "bg-white text-slate-700 border border-slate-200 hover:bg-slate-50 hover:border-slate-300 shadow-xs active:scale-[0.98]",
  outline:
    "bg-transparent text-slate-700 border border-slate-200 hover:bg-slate-50 hover:border-slate-300 active:scale-[0.98]",
  ghost:
    "bg-transparent text-slate-600 hover:bg-slate-100 hover:text-slate-900 active:scale-[0.98]",
  danger:
    "bg-rose-600 text-white hover:bg-rose-700 shadow-md shadow-rose-600/20 active:scale-[0.98]",
  dark:
    "bg-slate-900 text-white hover:bg-slate-800 shadow-md shadow-slate-900/20 active:scale-[0.98]",
  social:
    "bg-white border-2 border-slate-100 text-slate-800 hover:border-slate-200 hover:bg-slate-50 shadow-xs active:scale-95",
};

const sizeClassMap: Record<string, string> = {
  sm: "px-3.5 py-1.5 text-xs font-semibold rounded-xl gap-1.5",
  default: "px-5 py-2.5 text-sm font-semibold rounded-2xl gap-2",
  md: "px-5 py-2.5 text-sm font-semibold rounded-2xl gap-2",
  lg: "px-6 py-3.5 text-base font-bold rounded-2xl gap-2.5",
  xl: "px-8 py-4.5 text-lg font-black rounded-2xl gap-3",
  icon: "p-2.5 aspect-square rounded-2xl flex items-center justify-center",
};

const spinnerSizeMap: Record<string, string> = {
  sm: "h-3.5 w-3.5",
  default: "h-4 w-4",
  md: "h-4 w-4",
  lg: "h-5 w-5",
  xl: "h-6 w-6",
  icon: "h-5 w-5",
};

export default function Button({
  variant = "primary",
  size = "default",
  isLoading = false,
  leftIcon,
  rightIcon,
  fullWidth = false,
  disabled,
  className = "",
  children,
  type = "button",
  ...props
}: ButtonProps) {
  const sizeClass = sizeClassMap[size] || sizeClassMap.default;
  const spinnerClass = spinnerSizeMap[size] || spinnerSizeMap.default;
  const isButtonDisabled = disabled || isLoading;

  return (
    <button
      type={type}
      disabled={isButtonDisabled}
      aria-busy={isLoading ? "true" : undefined}
      className={`inline-flex items-center justify-center font-semibold transition-all select-none outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60 disabled:pointer-events-none disabled:active:scale-100 ${
        fullWidth ? "w-full" : ""
      } ${variantClassMap[variant] || variantClassMap.primary} ${sizeClass} ${className}`}
      {...props}
    >
      {isLoading ? (
        <Loader2 className={`animate-spin ${spinnerClass}`} aria-hidden="true" />
      ) : (
        leftIcon
      )}
      {children}
      {!isLoading && rightIcon}
    </button>
  );
}

export { Button };
