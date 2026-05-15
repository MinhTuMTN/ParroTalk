"use client";

type FloatingToastProps = {
  message: string;
  variant?: "info" | "success" | "warning" | "error";
};

const variantClassMap = {
  info: "border-sky-200 bg-sky-50 text-sky-700",
  success: "border-emerald-200 bg-emerald-50 text-emerald-700",
  warning: "border-amber-200 bg-amber-50 text-amber-700",
  error: "border-rose-200 bg-rose-50 text-rose-700",
};

export default function FloatingToast({ message, variant = "info" }: FloatingToastProps) {
  return (
    <div className="pointer-events-none fixed right-4 top-4 z-[70]">
      <div className={`rounded-xl border px-4 py-3 text-sm font-medium shadow-lg ${variantClassMap[variant]}`}>
        {message}
      </div>
    </div>
  );
}
