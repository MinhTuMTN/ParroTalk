"use client";

import React, { createContext, useContext, useEffect, useState } from "react";

interface User {
  id: string;
  email: string;
  fullName: string;
  role: string;
  avatarUrl?: string | null;
  emailVerified: boolean;
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (token: string, refreshToken: string, user: User) => void;
  logout: () => void;
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

    localStorage.setItem("token", token);
    localStorage.setItem("refreshToken", refreshToken);
    localStorage.setItem("user", JSON.stringify(user));
    setUser(user);
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, isLoading, login, logout }}>
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

