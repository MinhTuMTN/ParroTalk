"use client";

export type SwitchProps = {
  checked: boolean;
  onChange?: (next: boolean) => void;
  onCheckedChange?: (next: boolean) => void;
  disabled?: boolean;
  "aria-label"?: string;
  className?: string;
};

export default function Switch({
  checked,
  onChange,
  onCheckedChange,
  disabled = false,
  "aria-label": ariaLabel,
  className = "",
}: SwitchProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={ariaLabel}
      disabled={disabled}
      onClick={() => {
        const next = !checked;
        onChange?.(next);
        onCheckedChange?.(next);
      }}
      className={`relative inline-flex h-7 w-12 items-center rounded-full transition-colors ${
        checked ? "bg-emerald-600" : "bg-slate-300 dark:bg-slate-700"
      } disabled:cursor-not-allowed disabled:opacity-60 ${className}`.trim()}
    >
      <span
        className={`inline-block h-5 w-5 transform rounded-full bg-white transition-transform ${
          checked ? "translate-x-6" : "translate-x-1"
        }`}
      />
    </button>
  );
}

export { Switch };
