"use client";

import { AlertCircle, CheckCircle2, Info, TriangleAlert, X } from "lucide-react";

type FloatingToastProps = {
  message: string;
  variant?: "info" | "success" | "warning" | "error";
  onClose?: () => void;
};

const variantClassMap = {
  info: "border-sky-200 bg-sky-50 text-sky-700",
  success: "border-emerald-200 bg-emerald-50 text-emerald-700",
  warning: "border-amber-200 bg-amber-50 text-amber-700",
  error: "border-rose-200 bg-rose-50 text-rose-700",
};

const iconMap = {
  info: Info,
  success: CheckCircle2,
  warning: TriangleAlert,
  error: AlertCircle,
};

export default function FloatingToast({ message, variant = "info", onClose }: FloatingToastProps) {
  const Icon = iconMap[variant];

  return (
    <div className="fixed right-4 top-4 z-[70] max-w-[calc(100vw-2rem)]" role="status" aria-live="polite">
      <div className={`flex max-w-sm items-start gap-3 rounded-xl border px-4 py-3 text-sm font-medium shadow-lg ${variantClassMap[variant]}`}>
        <Icon className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
        {message}
        {onClose ? (
          <button
            type="button"
            aria-label="Dismiss notification"
            onClick={onClose}
            className="-mr-1 rounded-md p-0.5 transition hover:bg-white/60"
          >
            <X className="h-4 w-4" aria-hidden="true" />
          </button>
        ) : null}
      </div>
    </div>
  );
}
