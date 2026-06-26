"use client";

import { X } from "lucide-react";
import Button from "@/components/ui/Button";

type TemporaryPasswordModalProps = {
  password: string | null;
  onClose: () => void;
};

export default function TemporaryPasswordModal({
  password,
  onClose,
}: TemporaryPasswordModalProps) {
  if (!password) return null;

  return (
    <div className="fixed inset-0 z-[95] flex items-center justify-center p-4">
      <button
        type="button"
        className="absolute inset-0 bg-slate-950/40 backdrop-blur-sm"
        onClick={onClose}
        aria-label="Close temporary password modal"
      />
      <div className="relative w-full max-w-md rounded-[28px] border border-slate-200 bg-white p-6 shadow-2xl">
        <button
          type="button"
          onClick={onClose}
          className="absolute right-5 top-5 rounded-full p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
          aria-label="Close"
        >
          <X className="h-5 w-5" />
        </button>

        <h2 className="text-xl font-bold text-slate-950">Temporary password</h2>
        <p className="mt-2 text-sm text-slate-500">
          Share this password securely with the user. It will not be shown again.
        </p>
        <div className="mt-5 rounded-2xl border border-emerald-100 bg-emerald-50 px-4 py-3 font-mono text-lg font-semibold text-emerald-800">
          {password}
        </div>
        <div className="mt-6 flex justify-end">
          <Button onClick={onClose}>Done</Button>
        </div>
      </div>
    </div>
  );
}
