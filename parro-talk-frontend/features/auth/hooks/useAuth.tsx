"use client";

import React, { createContext, useCallback, useContext, useEffect, useState } from "react";

export const AUTH_CHANGED_EVENT = "parrotalk:auth-changed";
const USER_SETTINGS_STORAGE_KEY = "parrotalk_user_settings";
const VIDEO_LOOP_STORAGE_KEY = "parrotalk_video_loop";

interface User {
  id: string;
  email: string;
  fullName: string;
  username?: string;
  role: string;
  avatarUrl?: string | null;
  emailVerified: boolean;
  hasPassword?: boolean;
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (token: string, refreshToken: string, user: User) => void;
  logout: () => void;
  updateUser: (partial: Partial<User>) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const savedUser = localStorage.getItem("user");
    const token = localStorage.getItem("token");

    let parsedUser: User | null = null;
    let shouldClear = false;

    if (savedUser && token) {
      try {
        parsedUser = JSON.parse(savedUser);
      } catch {
        shouldClear = true;
      }
    }

    if (shouldClear) {
      localStorage.removeItem("token");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("user");
    }

    setTimeout(() => {
      if (parsedUser) {
        setUser(parsedUser);
      }
      setIsLoading(false);
    }, 0);
  }, []);

  const login = (token: string, refreshToken: string, user: User) => {
    if (!user) {
      throw new Error("Cannot log in without user data");
    }

    localStorage.removeItem(USER_SETTINGS_STORAGE_KEY);
    localStorage.removeItem(VIDEO_LOOP_STORAGE_KEY);

    localStorage.setItem("token", token);
    localStorage.setItem("refreshToken", refreshToken);
    localStorage.setItem("user", JSON.stringify(user));
    setUser(user);
    window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
    localStorage.removeItem(USER_SETTINGS_STORAGE_KEY);
    localStorage.removeItem(VIDEO_LOOP_STORAGE_KEY);
    setUser(null);
    window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
  };

  const updateUser = useCallback(
    (partial: Partial<User>) => {
      if (!user) {
        return;
      }
      const updatedUser: User = { ...user, ...partial };
      localStorage.setItem("user", JSON.stringify(updatedUser));
      setUser(updatedUser);
    },
    [user]
  );

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, isLoading, login, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};

