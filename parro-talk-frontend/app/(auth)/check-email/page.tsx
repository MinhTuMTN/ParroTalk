"use client";

import axios from "axios";
import axiosInstance from "@/lib/axios";
import { ArrowLeft, Loader2, MailCheck, Send } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";

export default function CheckEmailPage() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("We sent a verification link to your inbox.");
  const [error, setError] = useState("");
  const [isSending, setIsSending] = useState(false);

  useEffect(() => {
    const queryEmail = new URLSearchParams(window.location.search).get("email");
    if (queryEmail) {
      setEmail(queryEmail);
    }
  }, []);

  const handleResend = async () => {
    if (!email) {
      setError("Enter your email address to request another verification link.");
      return;
    }

    setIsSending(true);
    setError("");

    try {
      const response = await axiosInstance.post("/auth/resend-verification-email", { email });
      setMessage(response.data.message || "If an account exists for that email, a verification email has been sent.");
    } catch (err: unknown) {
      const data = axios.isAxiosError<{ message?: string }>(err) ? err.response?.data : undefined;
      setError(data?.message || "Unable to resend the verification email right now.");
    } finally {
      setIsSending(false);
    }
  };

  return (
    <main className="min-h-screen bg-[#FDFDFD] flex items-center justify-center p-6 font-sans">
      <section className="w-full max-w-xl bg-white border border-gray-100 rounded-[2rem] shadow-xl p-8 md:p-10">
        <div className="flex flex-col items-center text-center gap-4">
          <span className="inline-flex h-14 w-14 items-center justify-center rounded-full bg-green-50 text-green-600">
            <MailCheck className="h-7 w-7" />
          </span>
          <div className="space-y-2">
            <h1 className="text-3xl font-black text-gray-900">Check your email</h1>
            <p className="text-gray-500 font-medium">{message}</p>
          </div>
        </div>

        <div className="mt-8 space-y-4">
          <label className="block text-[11px] font-black uppercase tracking-widest text-gray-400">
            Email address
          </label>
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="name@example.com"
            className="w-full rounded-2xl border-2 border-transparent bg-gray-50 px-4 py-4 text-[15px] font-bold text-gray-800 shadow-sm outline-none transition-all focus:border-green-500 focus:bg-white"
          />

          {error ? (
            <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-bold text-red-600">
              {error}
            </div>
          ) : null}

          <button
            type="button"
            onClick={handleResend}
            disabled={isSending}
            className="flex w-full items-center justify-center gap-2 rounded-2xl bg-green-500 py-4 font-black text-white shadow-lg shadow-green-100 transition-all hover:bg-green-600 disabled:opacity-70"
          >
            {isSending ? <Loader2 className="h-5 w-5 animate-spin" /> : <Send className="h-5 w-5" />}
            Resend verification email
          </button>
        </div>

        <div className="mt-6 flex justify-center">
          <Link href="/login" className="inline-flex items-center gap-2 font-bold text-green-600 hover:text-green-700">
            <ArrowLeft className="h-4 w-4" />
            Back to login
          </Link>
        </div>
      </section>
    </main>
  );
}
