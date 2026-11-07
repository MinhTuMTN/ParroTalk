"use client";

import type { ButtonHTMLAttributes, ReactNode } from "react";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger" | "outline";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  leftIcon?: ReactNode;
  size?: "default" | "sm" | "lg" | "icon" | string;
};

const variantClassMap: Record<ButtonVariant, string> = {
  primary:
    "bg-emerald-600 text-white hover:bg-emerald-700 shadow-[0_10px_30px_rgba(16,185,129,0.28)]",
  secondary: "bg-white text-slate-700 border border-slate-200 hover:bg-slate-50",
  outline: "bg-transparent text-slate-700 border border-slate-200 hover:bg-slate-50",
  ghost: "bg-transparent text-slate-600 hover:bg-slate-100",
  danger: "bg-rose-600 text-white hover:bg-rose-700",
};

const sizeClassMap: Record<string, string> = {
  default: "px-5 py-2.5",
  sm: "px-3 py-1.5 text-xs",
  lg: "px-8 py-4 text-base",
  icon: "p-2", // smaller padding for icon buttons
};

export default function Button({
  variant = "primary",
  size = "default",
  leftIcon,
  className = "",
  children,
  ...props
}: ButtonProps) {
  const sizeClass = sizeClassMap[size] || sizeClassMap.default;
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-2xl font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${variantClassMap[variant]} ${sizeClass} ${className}`}
      {...props}
    >
      {leftIcon}
      {children}
    </button>
  );
}

export { Button };

