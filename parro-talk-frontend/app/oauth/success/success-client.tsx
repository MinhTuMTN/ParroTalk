"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

import { useAuth } from "@/features/auth/hooks/useAuth";
import axiosInstance from "@/lib/axios";

export default function OAuthSuccessClient() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuth();
  const hasStarted = useRef(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (hasStarted.current) {
      return;
    }

    hasStarted.current = true;

    const accessToken = searchParams.get("accessToken");
    const refreshToken = searchParams.get("refreshToken");

    if (!accessToken || !refreshToken) {
      router.replace("/login?error=oauth_failed");
      return;
    }

    const completeLogin = async () => {
      try {
        const response = await axiosInstance.get("/auth/me", {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        });
        const user = response.data.result;

        if (!user) {
          throw new Error("OAuth login did not return user data");
        }

        login(accessToken, refreshToken, user);
        router.replace("/library");
      } catch {
        setError("Unable to finish Google login. Please try again.");
        router.replace("/login?error=oauth_failed");
      }
    };

    completeLogin();
  }, [login, router, searchParams]);

  return (
    <div className="min-h-screen bg-[#FDFDFD] flex items-center justify-center p-6">
      <div className="text-sm font-bold text-gray-500">
        {error || "Signing you in..."}
      </div>
    </div>
  );
}
