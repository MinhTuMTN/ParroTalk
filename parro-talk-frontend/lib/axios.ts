import axios from "axios";

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://127.0.0.1:8080/api";
export const BACKEND_URL = process.env.NEXT_PUBLIC_BACKEND_URL ?? API_URL.replace(/\/api\/?$/, "");

const axiosInstance = axios.create({
  baseURL: API_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor: attach token
axiosInstance.interceptors.request.use(
  (config) => {
    if (typeof window !== "undefined") {
      const token = localStorage.getItem("token");
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export const RATE_LIMIT_EVENT = "parrotalk:rate-limit";

export interface RateLimitEventDetail {
  retryAfterSeconds: number;
  message: string;
}

function parseRetryAfterSeconds(headers: Record<string, unknown> | undefined): number {
  if (!headers) {
    return 60;
  }

  const rawRetryAfter = headers["retry-after"] ?? headers["Retry-After"];
  if (rawRetryAfter !== undefined && rawRetryAfter !== null) {
    const parsed = Number.parseInt(String(rawRetryAfter), 10);
    if (Number.isFinite(parsed) && parsed > 0) {
      return parsed;
    }
  }

  const rawReset = headers["x-ratelimit-reset"] ?? headers["X-RateLimit-Reset"];
  if (rawReset !== undefined && rawReset !== null) {
    const resetEpoch = Number.parseInt(String(rawReset), 10);
    if (Number.isFinite(resetEpoch)) {
      const diff = resetEpoch - Math.floor(Date.now() / 1000);
      if (diff > 0) {
        return diff;
      }
    }
  }

  return 60;
}

const DEFAULT_RATE_LIMIT_MESSAGE =
  "Bạn đang gửi yêu cầu quá nhanh. Vui lòng chờ một lát rồi thử lại.";

function dispatchRateLimitEvent(response: {
  headers?: unknown;
  data?: { message?: unknown };
}): void {
  if (typeof window === "undefined") {
    return;
  }
  const retryAfterSeconds = parseRetryAfterSeconds(
    response.headers as Record<string, unknown> | undefined
  );
  const rawMessage =
    typeof response.data?.message === "string"
      ? response.data.message.trim()
      : "";
  const message =
    rawMessage.length > 0 &&
    rawMessage !== "Too many requests. Please try again later."
      ? rawMessage
      : DEFAULT_RATE_LIMIT_MESSAGE;

  window.dispatchEvent(
    new CustomEvent<RateLimitEventDetail>(RATE_LIMIT_EVENT, {
      detail: { retryAfterSeconds, message },
    })
  );
}

// Response interceptor: handle 401 refresh and 429 rate limiting
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 429) {
      dispatchRateLimitEvent(error.response);
      return Promise.reject(error);
    }

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      const refreshToken = typeof window !== "undefined" ? localStorage.getItem("refreshToken") : null;

      if (refreshToken) {
        try {
          const response = await axios.post(`${API_URL}/auth/refresh`, { refreshToken });
          const { token: newToken } = response.data.result;
          
          if (typeof window !== "undefined") {
            localStorage.setItem("token", newToken);
          }
          axiosInstance.defaults.headers.common["Authorization"] = `Bearer ${newToken}`;
          
          return axiosInstance(originalRequest);
        } catch (refreshError) {
          if (axios.isAxiosError(refreshError) && refreshError.response?.status === 429) {
            dispatchRateLimitEvent(refreshError.response);
            return Promise.reject(refreshError);
          }

          // Refresh failed, logout user
          if (typeof window !== "undefined") {
            localStorage.removeItem("token");
            localStorage.removeItem("refreshToken");
            localStorage.removeItem("user");
            localStorage.removeItem("parrotalk_user_settings");
            localStorage.removeItem("parrotalk_video_loop");
            window.dispatchEvent(new Event("parrotalk:auth-changed"));

            const publicPaths = ["/", "/login", "/register", "/forgot-password", "/reset-password", "/verify-email"];
            if (!publicPaths.includes(window.location.pathname)) {
              window.location.href = "/login";
            }
          }
        }
      } else {
        // If user has no refresh token, only redirect if they are on a protected route
        if (typeof window !== "undefined") {
          const publicPaths = ["/", "/login", "/register", "/forgot-password", "/reset-password", "/verify-email"];
          if (!publicPaths.includes(window.location.pathname)) {
            window.location.href = "/login";
          }
        }
      }
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;
