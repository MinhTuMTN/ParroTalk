"use client";

import type { ButtonHTMLAttributes, ReactNode } from "react";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  leftIcon?: ReactNode;
};

const variantClassMap: Record<ButtonVariant, string> = {
  primary:
    "bg-emerald-600 text-white hover:bg-emerald-700 shadow-[0_10px_30px_rgba(16,185,129,0.28)]",
  secondary: "bg-white text-slate-700 border border-slate-200 hover:bg-slate-50",
  ghost: "bg-transparent text-slate-600 hover:bg-slate-100",
  danger: "bg-rose-600 text-white hover:bg-rose-700",
};

export default function Button({
  variant = "primary",
  leftIcon,
  className = "",
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-2xl px-5 py-2.5 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${variantClassMap[variant]} ${className}`}
      {...props}
    >
      {leftIcon}
      {children}
    </button>
  );
}
