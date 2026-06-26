"use client";

import type { ReactNode } from "react";
import { AlertCircle } from "lucide-react";

type FormValidationProps = {
  message?: string;
  children?: ReactNode;
};

export function FormValidation({ message, children }: FormValidationProps) {
  if (!message && !children) {
    return null;
  }

  return (
    <div
      className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-bold text-red-600"
      role="alert"
    >
      <div className="flex items-start gap-2">
        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
        <div className="space-y-2">
          {message ? <p>{message}</p> : null}
          {children}
        </div>
      </div>
    </div>
  );
}

type FormFieldErrorProps = {
  id: string;
  message?: string;
};

export function FormFieldError({ id, message }: FormFieldErrorProps) {
  if (!message) {
    return null;
  }

  return (
    <p id={id} className="ml-1 text-xs font-bold text-red-500" role="alert">
      {message}
    </p>
  );
}
