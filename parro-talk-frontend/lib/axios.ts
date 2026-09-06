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

// Response interceptor: handle 401 and refresh
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

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
        } catch {
          // Refresh failed, logout user
          if (typeof window !== "undefined") {
            localStorage.removeItem("token");
            localStorage.removeItem("refreshToken");
            localStorage.removeItem("user");

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
