"use client";

import { FormFieldError, FormValidation } from "@/components/ui/FormValidation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import axiosInstance, { BACKEND_URL } from "@/lib/axios";
import axios from "axios";
import { ArrowRight, Loader2, Lock, Mail } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import React, { useEffect, useState } from "react";

type LoginFormErrors = {
  email?: string;
  password?: string;
};

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validateLoginForm(email: string, password: string): LoginFormErrors {
  const errors: LoginFormErrors = {};
  const normalizedEmail = email.trim();

  if (!normalizedEmail) {
    errors.email = "Email is required.";
  } else if (!emailPattern.test(normalizedEmail)) {
    errors.email = "Enter a valid email address.";
  }

  if (!password) {
    errors.password = "Password is required.";
  }

  return errors;
}

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [formErrors, setFormErrors] = useState<LoginFormErrors>({});
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [needsVerification, setNeedsVerification] = useState(false);

  useEffect(() => {
    const errorCode = new URLSearchParams(window.location.search).get("error");
    if (errorCode === "oauth_failed") {
      const message = "Google login failed. Please try again.";
      setError(message);
    }
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const validationErrors = validateLoginForm(email, password);

    if (Object.keys(validationErrors).length > 0) {
      setFormErrors(validationErrors);
      setError("Please fix the highlighted fields.");
      setNeedsVerification(false);
      return;
    }

    setIsLoading(true);
    setError("");
    setFormErrors({});
    setNeedsVerification(false);

    try {
      const response = await axiosInstance.post("/auth/login", { email: email.trim(), password });
      const { token, refreshToken, user } = response.data.result;

      login(token, refreshToken, user);
      router.push("/library");
    } catch (err: unknown) {
      const errorResponse = axios.isAxiosError<{ message?: string; errorCode?: string }>(err)
        ? err.response?.data
        : undefined;
      const message = errorResponse?.message;
      if (errorResponse?.errorCode === "EMAIL_NOT_VERIFIED") {
        setNeedsVerification(true);
      }
      const fallbackMessage = errorResponse?.errorCode === "EMAIL_NOT_VERIFIED"
        ? "Please verify your email before logging in."
        : "Invalid credentials. Please try again.";
      const loginError = message || fallbackMessage;
      setError(loginError);
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleLogin = () => {
    window.location.href = `${BACKEND_URL}/oauth2/authorization/google`;
  };

  return (
    <div className="min-h-screen bg-[#FDFDFD] flex items-center justify-center p-6 relative overflow-hidden font-sans">
      {/* Abstract Background Orbs */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-green-50 rounded-full blur-[120px] opacity-60 animate-pulse" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-blue-50 rounded-full blur-[120px] opacity-60 animate-pulse delay-700" />

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
            <h1 className="text-4xl font-black text-gray-900">Welcome Back</h1>
            <p className="text-gray-500 font-medium italic">Dictation in the flow of light.</p>
          </div>

          <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-6">
            <FormValidation message={error}>
              {needsVerification ? (
                <Link
                  href={`/check-email?email=${encodeURIComponent(email)}`}
                  className="inline-flex text-green-600 hover:text-green-700 underline underline-offset-4"
                >
                  Resend verification email
                </Link>
              ) : null}
            </FormValidation>

            <div className="space-y-4">
              <div className="space-y-1.5 px-2">
                <label htmlFor="login-email" className="text-[11px] font-black text-gray-400 uppercase tracking-widest ml-1">Email Address</label>
                <div className="relative group">
                  <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-green-500 transition-colors" />
                  <input
                    id="login-email"
                    type="email"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      setFormErrors((current) => ({ ...current, email: undefined }));
                    }}
                    placeholder="name@example.com"
                    aria-invalid={Boolean(formErrors.email)}
                    aria-describedby={formErrors.email ? "login-email-error" : undefined}
                    className={`w-full bg-gray-50 border-2 rounded-2xl pl-12 pr-4 py-4 text-[15px] font-bold text-gray-800 placeholder:text-gray-400 focus:outline-none focus:bg-white transition-all shadow-sm ${
                      formErrors.email
                        ? "border-red-300 focus:border-red-500"
                        : "border-transparent focus:border-green-500"
                    }`}
                  />
                </div>
                <FormFieldError id="login-email-error" message={formErrors.email} />
              </div>

              <div className="space-y-1.5 px-2">
                <div className="flex justify-between items-center ml-1">
                  <label htmlFor="login-password" className="text-[11px] font-black text-gray-400 uppercase tracking-widest">Password</label>
                  <Link href="#" className="text-[11px] font-black text-green-500 hover:text-green-600 uppercase tracking-widest">Forgot Password?</Link>
                </div>
                <div className="relative group">
                  <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-green-500 transition-colors" />
                  <input
                    id="login-password"
                    type="password"
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      setFormErrors((current) => ({ ...current, password: undefined }));
                    }}
                    placeholder="••••••••"
                    aria-invalid={Boolean(formErrors.password)}
                    aria-describedby={formErrors.password ? "login-password-error" : undefined}
                    className={`w-full bg-gray-50 border-2 rounded-2xl pl-12 pr-4 py-4 text-[15px] font-bold text-gray-800 placeholder:text-gray-400 focus:outline-none focus:bg-white transition-all shadow-sm ${
                      formErrors.password
                        ? "border-red-300 focus:border-red-500"
                        : "border-transparent focus:border-green-500"
                    }`}
                  />
                </div>
                <FormFieldError id="login-password-error" message={formErrors.password} />
              </div>
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
                  Login
                  <ArrowRight size={20} />
                </>
              )}
            </button>
          </form>

          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-100"></div>
            </div>
            <div className="relative flex justify-center text-[10px] font-black uppercase tracking-widest">
              <span className="bg-white px-4 text-gray-400">Or continue with</span>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <button
              type="button"
              onClick={handleGoogleLogin}
              className="h-12 md:h-14 bg-white border-2 border-slate-100 rounded-2xl flex items-center justify-center gap-3 font-bold text-[#1E293B] hover:border-[#F1F5F9] hover:bg-[#F1F5F9] transition-all active:scale-95 text-sm md:text-base"
            >
              <Image src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" width={20} height={20} />
              Google
            </button>
            <button className="h-12 md:h-14 bg-white border-2 border-slate-100 rounded-2xl flex items-center justify-center gap-3 font-bold text-[#1E293B] hover:border-[#F1F5F9] hover:bg-[#F1F5F9] transition-all active:scale-95 text-sm md:text-base">
              <Image src="https://www.svgrepo.com/show/452196/facebook-1.svg" alt="Facebook" width={20} height={20} />
              Facebook
            </button>
          </div>

          <p className="text-center text-sm font-bold text-gray-500">
            Don&apos;t have an account?{" "}
            <Link href="/register" className="text-green-500 hover:text-green-600 underline underline-offset-4 decoration-2">
              Sign Up
            </Link>
          </p>
        </div>

        <div className="flex justify-center gap-6 mt-12 text-[10px] font-black uppercase tracking-widest text-gray-400">
          <Link href="#" className="hover:text-gray-600 transition-colors">Privacy Policy</Link>
          <Link href="#" className="hover:text-gray-600 transition-colors">Terms of Service</Link>
          <Link href="#" className="hover:text-gray-600 transition-colors">Help Center</Link>
        </div>
        <p className="text-center text-[10px] font-black text-gray-300 uppercase tracking-widest mt-8">
          © 2026 ParroTalk. The Digital Aurora.
        </p>
      </div>
    </div>
  );
}

