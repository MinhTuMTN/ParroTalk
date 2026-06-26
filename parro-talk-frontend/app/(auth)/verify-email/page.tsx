"use client";

import axios from "axios";
import axiosInstance from "@/lib/axios";
import { ArrowRight, CircleCheck, CircleX, Loader2 } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";

type VerificationState = "loading" | "success" | "error";

export default function VerifyEmailPage() {
  const [state, setState] = useState<VerificationState>("loading");
  const [message, setMessage] = useState("Verifying your email...");

  useEffect(() => {
    const token = new URLSearchParams(window.location.search).get("token");
    if (!token) {
      setState("error");
      setMessage("Verification token is missing.");
      return;
    }

    const verifyEmail = async () => {
      try {
        const response = await axiosInstance.post("/auth/verify-email", { token });
        setMessage(response.data.message || "Email verified successfully.");
        setState("success");
      } catch (err: unknown) {
        const data = axios.isAxiosError<{ message?: string }>(err) ? err.response?.data : undefined;
        setMessage(data?.message || "This verification link is invalid or has expired.");
        setState("error");
      }
    };

    void verifyEmail();
  }, []);

  return (
    <main className="min-h-screen bg-[#FDFDFD] flex items-center justify-center p-6 font-sans">
      <section className="w-full max-w-xl rounded-[2rem] border border-gray-100 bg-white p-8 text-center shadow-xl md:p-10">
        <div className="flex flex-col items-center gap-4">
          {state === "loading" ? (
            <span className="inline-flex h-14 w-14 items-center justify-center rounded-full bg-blue-50 text-blue-600">
              <Loader2 className="h-7 w-7 animate-spin" />
            </span>
          ) : null}
          {state === "success" ? (
            <span className="inline-flex h-14 w-14 items-center justify-center rounded-full bg-green-50 text-green-600">
              <CircleCheck className="h-7 w-7" />
            </span>
          ) : null}
          {state === "error" ? (
            <span className="inline-flex h-14 w-14 items-center justify-center rounded-full bg-red-50 text-red-600">
              <CircleX className="h-7 w-7" />
            </span>
          ) : null}

          <div className="space-y-2">
            <h1 className="text-3xl font-black text-gray-900">
              {state === "success" ? "Email verified" : state === "error" ? "Verification failed" : "Verifying email"}
            </h1>
            <p className="font-medium text-gray-500">{message}</p>
          </div>
        </div>

        {state !== "loading" ? (
          <div className="mt-8">
            <Link
              href="/login"
              className="inline-flex items-center gap-2 rounded-2xl bg-green-500 px-5 py-3 font-black text-white shadow-lg shadow-green-100 transition-all hover:bg-green-600"
            >
              Back to login
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        ) : null}
      </section>
    </main>
  );
}
