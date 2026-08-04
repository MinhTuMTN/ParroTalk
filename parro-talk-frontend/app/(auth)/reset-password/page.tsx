"use client";

import { FormFieldError, FormValidation } from "@/components/ui/FormValidation";
import FloatingToast from "@/components/ui/FloatingToast";
import axiosInstance from "@/lib/axios";
import axios from "axios";
import {
  ArrowRight,
  CheckCircle2,
  CircleX,
  Clock,
  Eye,
  EyeOff,
  Loader2,
  Lock,
  ShieldAlert,
} from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import React, { useEffect, useMemo, useState } from "react";

// ── Password strength helpers ───────────────────────────────────────────────

type PasswordRule = { label: string; test: (pw: string) => boolean };

const PASSWORD_RULES: PasswordRule[] = [
  { label: "At least 8 characters", test: (pw) => pw.length >= 8 },
  { label: "Has uppercase letter", test: (pw) => /[A-Z]/.test(pw) },
  { label: "Has lowercase letter", test: (pw) => /[a-z]/.test(pw) },
  { label: "Has a number", test: (pw) => /\d/.test(pw) },
  { label: "Has special character", test: (pw) => /[^a-zA-Z\d]/.test(pw) },
];

function getStrength(pw: string): { score: number; label: string; color: string } {
  const passed = PASSWORD_RULES.filter((r) => r.test(pw)).length;
  if (passed <= 1) return { score: passed, label: "Weak", color: "bg-red-500" };
  if (passed <= 2) return { score: passed, label: "Fair", color: "bg-orange-500" };
  if (passed <= 3) return { score: passed, label: "Good", color: "bg-yellow-500" };
  if (passed === 4) return { score: passed, label: "Strong", color: "bg-emerald-400" };
  return { score: passed, label: "Very Strong", color: "bg-green-500" };
}

// ── Token verification states ───────────────────────────────────────────────

type TokenStatus = "loading" | "valid" | "expired" | "used" | "invalid";

type ResetFormErrors = {
  newPassword?: string;
  confirmPassword?: string;
};

// ── Component ───────────────────────────────────────────────────────────────

export default function ResetPasswordPage() {
  const [tokenStatus, setTokenStatus] = useState<TokenStatus>("loading");
  const [token, setToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [formErrors, setFormErrors] = useState<ResetFormErrors>({});
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [toast, setToast] = useState<{ message: string; variant: "error" | "success" } | null>(
    null,
  );

  const strength = useMemo(() => getStrength(newPassword), [newPassword]);

  // ── Verify token on mount ──────────────────────────────────────────────

  useEffect(() => {
    const urlToken = new URLSearchParams(window.location.search).get("token");
    if (!urlToken) {
      setTokenStatus("invalid");
      return;
    }
    setToken(urlToken);

    const verify = async () => {
      try {
        const res = await axiosInstance.get("/auth/reset-password/verify", {
          params: { token: urlToken },
        });
        const status: string = res.data.result?.status ?? "invalid";
        setTokenStatus(status as TokenStatus);
      } catch {
        setTokenStatus("invalid");
      }
    };

    void verify();
  }, []);

  // ── Validate form ──────────────────────────────────────────────────────

  function validate(): ResetFormErrors {
    const errs: ResetFormErrors = {};
    if (!newPassword) {
      errs.newPassword = "New password is required.";
    } else if (newPassword.length < 8) {
      errs.newPassword = "Password must be at least 8 characters.";
    } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d]).+$/.test(newPassword)) {
      errs.newPassword = "Password must contain uppercase, lowercase, number, and special character.";
    }

    if (!confirmPassword) {
      errs.confirmPassword = "Confirm password is required.";
    } else if (newPassword !== confirmPassword) {
      errs.confirmPassword = "Passwords do not match.";
    }
    return errs;
  }

  // ── Submit ─────────────────────────────────────────────────────────────

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) {
      setFormErrors(errs);
      setError("Please fix the highlighted fields.");
      return;
    }

    setIsLoading(true);
    setError("");
    setFormErrors({});

    try {
      await axiosInstance.post("/auth/reset-password", {
        token,
        newPassword,
        confirmPassword,
      });
      setIsSuccess(true);
      setToast({ message: "Password changed successfully!", variant: "success" });
    } catch (err: unknown) {
      const data = axios.isAxiosError<{ message?: string; errorCode?: string }>(err)
        ? err.response?.data
        : undefined;

      if (data?.errorCode === "RESET_TOKEN_EXPIRED") {
        setTokenStatus("expired");
      } else if (data?.errorCode === "RESET_TOKEN_USED") {
        setTokenStatus("used");
      } else if (data?.errorCode === "RESET_TOKEN_INVALID") {
        setTokenStatus("invalid");
      } else {
        setError(data?.message || "Something went wrong. Please try again.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  // ── Render helpers ─────────────────────────────────────────────────────

  const renderStatusPage = (
    icon: React.ReactNode,
    iconBg: string,
    title: string,
    description: string,
    extra?: React.ReactNode,
  ) => (
    <div className="flex flex-col items-center gap-4 text-center">
      <span className={`inline-flex h-14 w-14 items-center justify-center rounded-full ${iconBg}`}>
        {icon}
      </span>
      <div className="space-y-2">
        <h1 className="text-3xl font-black text-gray-900">{title}</h1>
        <p className="font-medium text-gray-500">{description}</p>
      </div>
      <div className="mt-4 flex flex-col gap-3">{extra}</div>
    </div>
  );

  // ── Main render ────────────────────────────────────────────────────────

  return (
    <div className="min-h-screen bg-[#FDFDFD] flex items-center justify-center p-6 relative overflow-hidden font-sans">
      {/* Abstract Background Orbs */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-green-50 rounded-full blur-[120px] opacity-60 animate-pulse" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-blue-50 rounded-full blur-[120px] opacity-60 animate-pulse delay-700" />

      {toast ? (
        <FloatingToast message={toast.message} variant={toast.variant} onClose={() => setToast(null)} />
      ) : null}

      <div className="w-full max-w-xl relative z-10">
        <div className="bg-white p-8 rounded-[3rem] shadow-2xl border border-gray-100 flex flex-col gap-8">
          {/* Logo */}
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

          {/* ── Loading ─────────────────────────────────────────────── */}
          {tokenStatus === "loading" ? (
            <div className="flex flex-col items-center gap-4 py-8">
              <span className="inline-flex h-14 w-14 items-center justify-center rounded-full bg-blue-50 text-blue-600">
                <Loader2 className="h-7 w-7 animate-spin" />
              </span>
              <p className="font-black text-gray-900 text-xl">Verifying your link…</p>
            </div>
          ) : null}

          {/* ── Invalid ─────────────────────────────────────────────── */}
          {tokenStatus === "invalid"
            ? renderStatusPage(
                <CircleX className="h-7 w-7" />,
                "bg-red-50 text-red-600",
                "Invalid reset link",
                "This password reset link is invalid or has already been used.",
                <Link
                  href="/login"
                  className="inline-flex items-center gap-2 rounded-2xl bg-green-500 px-5 py-3 font-black text-white shadow-lg shadow-green-100 transition-all hover:bg-green-600"
                >
                  Back to Login
                  <ArrowRight className="h-4 w-4" />
                </Link>,
              )
            : null}

          {/* ── Expired ─────────────────────────────────────────────── */}
          {tokenStatus === "expired"
            ? renderStatusPage(
                <Clock className="h-7 w-7" />,
                "bg-amber-50 text-amber-600",
                "Link expired",
                "This password reset link has expired. Please request a new one.",
                <>
                  <Link
                    href="/forgot-password"
                    className="inline-flex items-center gap-2 rounded-2xl bg-green-500 px-5 py-3 font-black text-white shadow-lg shadow-green-100 transition-all hover:bg-green-600"
                  >
                    Request new link
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                  <Link
                    href="/login"
                    className="inline-flex items-center gap-1.5 text-sm font-bold text-gray-500 hover:text-gray-700 transition-colors"
                  >
                    Back to Login
                  </Link>
                </>,
              )
            : null}

          {/* ── Used ────────────────────────────────────────────────── */}
          {tokenStatus === "used"
            ? renderStatusPage(
                <ShieldAlert className="h-7 w-7" />,
                "bg-orange-50 text-orange-600",
                "Link already used",
                "This password reset link has already been used.",
                <Link
                  href="/login"
                  className="inline-flex items-center gap-2 rounded-2xl bg-green-500 px-5 py-3 font-black text-white shadow-lg shadow-green-100 transition-all hover:bg-green-600"
                >
                  Back to Login
                  <ArrowRight className="h-4 w-4" />
                </Link>,
              )
            : null}

          {/* ── Success ─────────────────────────────────────────────── */}
          {isSuccess
            ? renderStatusPage(
                <CheckCircle2 className="h-7 w-7" />,
                "bg-green-50 text-green-600",
                "Password changed",
                "Your password has been reset successfully. You can now log in with your new password.",
                <Link
                  href="/login"
                  className="inline-flex items-center gap-2 rounded-2xl bg-green-500 px-5 py-3 font-black text-white shadow-lg shadow-green-100 transition-all hover:bg-green-600"
                >
                  Go to Login
                  <ArrowRight className="h-4 w-4" />
                </Link>,
              )
            : null}

          {/* ── Valid — reset form ───────────────────────────────────── */}
          {tokenStatus === "valid" && !isSuccess ? (
            <>
              <div className="text-center space-y-2">
                <h1 className="text-4xl font-black text-gray-900">Reset Password</h1>
                <p className="text-gray-500 font-medium italic">
                  Create a strong new password for your account.
                </p>
              </div>

              <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-6">
                <FormValidation message={error} />

                {/* New password */}
                <div className="space-y-1.5 px-2">
                  <label
                    htmlFor="new-password"
                    className="text-[11px] font-black text-gray-400 uppercase tracking-widest ml-1"
                  >
                    New Password
                  </label>
                  <div className="relative group">
                    <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-green-500 transition-colors" />
                    <input
                      id="new-password"
                      type={showPassword ? "text" : "password"}
                      value={newPassword}
                      onChange={(e) => {
                        setNewPassword(e.target.value);
                        setFormErrors((c) => ({ ...c, newPassword: undefined }));
                      }}
                      placeholder="••••••••"
                      autoFocus
                      aria-invalid={Boolean(formErrors.newPassword)}
                      aria-describedby={formErrors.newPassword ? "new-pw-error" : undefined}
                      className={`w-full bg-gray-50 border-2 rounded-2xl pl-12 pr-12 py-4 text-[15px] font-bold text-gray-800 placeholder:text-gray-400 focus:outline-none focus:bg-white transition-all shadow-sm ${
                        formErrors.newPassword
                          ? "border-red-300 focus:border-red-500"
                          : "border-transparent focus:border-green-500"
                      }`}
                    />
                    <button
                      type="button"
                      tabIndex={-1}
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                      aria-label={showPassword ? "Hide password" : "Show password"}
                    >
                      {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                    </button>
                  </div>
                  <FormFieldError id="new-pw-error" message={formErrors.newPassword} />

                  {/* Password strength indicator */}
                  {newPassword.length > 0 ? (
                    <div className="mt-2 space-y-2 ml-1">
                      {/* Strength bar */}
                      <div className="flex items-center gap-2">
                        <div className="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                          <div
                            className={`h-full rounded-full transition-all duration-300 ${strength.color}`}
                            style={{ width: `${(strength.score / PASSWORD_RULES.length) * 100}%` }}
                          />
                        </div>
                        <span className="text-[10px] font-black uppercase tracking-wider text-gray-400 min-w-[70px] text-right">
                          {strength.label}
                        </span>
                      </div>

                      {/* Rule checklist */}
                      <ul className="space-y-0.5">
                        {PASSWORD_RULES.map((rule) => {
                          const passed = rule.test(newPassword);
                          return (
                            <li
                              key={rule.label}
                              className={`flex items-center gap-1.5 text-[11px] font-bold transition-colors ${
                                passed ? "text-green-500" : "text-gray-400"
                              }`}
                            >
                              <span
                                className={`inline-block w-3 h-3 rounded-full border-2 transition-all ${
                                  passed
                                    ? "bg-green-500 border-green-500"
                                    : "bg-transparent border-gray-300"
                                }`}
                              />
                              {rule.label}
                            </li>
                          );
                        })}
                      </ul>
                    </div>
                  ) : null}
                </div>

                {/* Confirm password */}
                <div className="space-y-1.5 px-2">
                  <label
                    htmlFor="confirm-password"
                    className="text-[11px] font-black text-gray-400 uppercase tracking-widest ml-1"
                  >
                    Confirm Password
                  </label>
                  <div className="relative group">
                    <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-green-500 transition-colors" />
                    <input
                      id="confirm-password"
                      type={showConfirm ? "text" : "password"}
                      value={confirmPassword}
                      onChange={(e) => {
                        setConfirmPassword(e.target.value);
                        setFormErrors((c) => ({ ...c, confirmPassword: undefined }));
                      }}
                      placeholder="••••••••"
                      aria-invalid={Boolean(formErrors.confirmPassword)}
                      aria-describedby={formErrors.confirmPassword ? "confirm-pw-error" : undefined}
                      className={`w-full bg-gray-50 border-2 rounded-2xl pl-12 pr-12 py-4 text-[15px] font-bold text-gray-800 placeholder:text-gray-400 focus:outline-none focus:bg-white transition-all shadow-sm ${
                        formErrors.confirmPassword
                          ? "border-red-300 focus:border-red-500"
                          : "border-transparent focus:border-green-500"
                      }`}
                    />
                    <button
                      type="button"
                      tabIndex={-1}
                      onClick={() => setShowConfirm(!showConfirm)}
                      className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                      aria-label={showConfirm ? "Hide password" : "Show password"}
                    >
                      {showConfirm ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                    </button>
                  </div>
                  <FormFieldError id="confirm-pw-error" message={formErrors.confirmPassword} />

                  {/* Match indicator */}
                  {confirmPassword.length > 0 && newPassword.length > 0 ? (
                    <p
                      className={`ml-1 text-[11px] font-bold transition-colors ${
                        newPassword === confirmPassword ? "text-green-500" : "text-red-400"
                      }`}
                    >
                      {newPassword === confirmPassword
                        ? "✓ Passwords match"
                        : "✗ Passwords do not match"}
                    </p>
                  ) : null}
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
                      Reset Password
                      <ArrowRight size={20} />
                    </>
                  )}
                </button>
              </form>
            </>
          ) : null}
        </div>

        <p className="text-center text-[10px] font-black text-gray-300 uppercase tracking-widest mt-8">
          © 2026 ParroTalk. The Digital Aurora.
        </p>
      </div>
    </div>
  );
}
