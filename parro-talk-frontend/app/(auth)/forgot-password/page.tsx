"use client";

import { FormFieldError, FormValidation } from "@/components/ui/FormValidation";
import FloatingToast from "@/components/ui/FloatingToast";
import axiosInstance from "@/lib/axios";
import axios from "axios";
import { ArrowLeft, CheckCircle2, Loader2, Mail, Send } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import React, { useState } from "react";

type ForgotFormErrors = {
  email?: string;
};

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validateEmail(email: string): ForgotFormErrors {
  const errors: ForgotFormErrors = {};
  const trimmed = email.trim();
  if (!trimmed) {
    errors.email = "Email is required.";
  } else if (!emailPattern.test(trimmed)) {
    errors.email = "Enter a valid email address.";
  }
  return errors;
}

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [formErrors, setFormErrors] = useState<ForgotFormErrors>({});
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [toast, setToast] = useState<{ message: string; variant: "error" | "success" } | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const validationErrors = validateEmail(email);

    if (Object.keys(validationErrors).length > 0) {
      setFormErrors(validationErrors);
      setError("Please fix the highlighted field.");
      return;
    }

    setIsLoading(true);
    setError("");
    setFormErrors({});

    try {
      await axiosInstance.post("/auth/forgot-password", { email: email.trim() });
      setIsSuccess(true);
    } catch (err: unknown) {
      const errorResponse = axios.isAxiosError<{ message?: string; errorCode?: string }>(err)
        ? err.response?.data
        : undefined;
      const message = errorResponse?.message;

      if (errorResponse?.errorCode === "RATE_LIMIT_EXCEEDED") {
        setToast({ message: "Too many requests. Please try again later.", variant: "error" });
      } else {
        setError(message || "Something went wrong. Please try again.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#FDFDFD] flex items-center justify-center p-6 relative overflow-hidden font-sans">
      {/* Abstract Background Orbs */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-green-50 rounded-full blur-[120px] opacity-60 animate-pulse" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-blue-50 rounded-full blur-[120px] opacity-60 animate-pulse delay-700" />

      {toast ? (
        <FloatingToast
          message={toast.message}
          variant={toast.variant}
          onClose={() => setToast(null)}
        />
      ) : null}

      <div className="w-full max-w-xl relative z-10">
        <div className="bg-white p-8 rounded-[3rem] shadow-2xl border border-gray-100 flex flex-col gap-8">
          <div className="text-center space-y-2">
            <div className="flex justify-center">
              <div className="relative w-80 h-28">
                <Image
                  src="/logo_long.png"
                  alt="ParroTalk"
                  fill
                  sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                  className="object-contain"
                  priority
                />
              </div>
            </div>

            {isSuccess ? (
              <>
                <div className="flex justify-center pt-2">
                  <span className="inline-flex h-14 w-14 items-center justify-center rounded-full bg-green-50 text-green-600">
                    <CheckCircle2 className="h-7 w-7" />
                  </span>
                </div>
                <h1 className="text-3xl font-black text-gray-900">Check your email</h1>
                <p className="text-gray-500 font-medium max-w-sm mx-auto">
                  If an account exists for <strong className="text-gray-700">{email.trim()}</strong>,
                  we&apos;ve sent a password reset link.
                </p>
                <p className="text-gray-400 text-sm font-medium">
                  The link will expire in 30 minutes.
                </p>
              </>
            ) : (
              <>
                <h1 className="text-4xl font-black text-gray-900">Forgot Password</h1>
                <p className="text-gray-500 font-medium italic">
                  Enter your email and we&apos;ll send you a reset link.
                </p>
              </>
            )}
          </div>

          {isSuccess ? (
            <div className="flex flex-col gap-4 items-center">
              <Link
                href="/login"
                className="inline-flex items-center gap-2 rounded-2xl bg-green-500 px-6 py-4 font-black text-white shadow-xl shadow-green-100 transition-all hover:bg-green-600 active:scale-[0.98] text-lg"
              >
                <ArrowLeft size={20} />
                Back to Login
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-6">
              <FormValidation message={error} />

              <div className="space-y-1.5 px-2">
                <label
                  htmlFor="forgot-email"
                  className="text-[11px] font-black text-gray-400 uppercase tracking-widest ml-1"
                >
                  Email Address
                </label>
                <div className="relative group">
                  <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-green-500 transition-colors" />
                  <input
                    id="forgot-email"
                    type="email"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      setFormErrors((c) => ({ ...c, email: undefined }));
                    }}
                    placeholder="name@example.com"
                    autoFocus
                    aria-invalid={Boolean(formErrors.email)}
                    aria-describedby={formErrors.email ? "forgot-email-error" : undefined}
                    className={`w-full bg-gray-50 border-2 rounded-2xl pl-12 pr-4 py-4 text-[15px] font-bold text-gray-800 placeholder:text-gray-400 focus:outline-none focus:bg-white transition-all shadow-sm ${
                      formErrors.email
                        ? "border-red-300 focus:border-red-500"
                        : "border-transparent focus:border-green-500"
                    }`}
                  />
                </div>
                <FormFieldError id="forgot-email-error" message={formErrors.email} />
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full bg-green-500 text-white font-black py-5 rounded-2xl shadow-xl shadow-green-100 hover:bg-green-600 transition-all active:scale-[0.98] flex items-center justify-center gap-2 text-lg disabled:opacity-70"
              >
                {isLoading ? (
                  <Loader2 className="w-6 h-6 animate-spin" />
                ) : (
                  <>
                    Send Reset Link
                    <Send size={18} />
                  </>
                )}
              </button>

              <div className="text-center">
                <Link
                  href="/login"
                  className="inline-flex items-center gap-1.5 text-sm font-bold text-gray-500 hover:text-gray-700 transition-colors"
                >
                  <ArrowLeft size={14} />
                  Back to Login
                </Link>
              </div>
            </form>
          )}
        </div>

        <p className="text-center text-[10px] font-black text-gray-300 uppercase tracking-widest mt-8">
          © 2026 ParroTalk. The Digital Aurora.
        </p>
      </div>
    </div>
  );
}
