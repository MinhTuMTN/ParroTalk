"use client";

import type { ReactNode } from "react";
import { useState } from "react";
import AdminHeader from "@/components/common/AdminHeader";
import Sidebar from "@/components/common/Sidebar";

type AdminShellProps = {
  children: ReactNode;
};

export default function AdminShell({ children }: AdminShellProps) {
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  return (
    <div className="min-h-screen bg-[#f5f8f7]">
      <div className="flex min-h-screen">
        <Sidebar
          collapsed={isSidebarCollapsed}
          onToggle={() => setIsSidebarCollapsed((value) => !value)}
          isMobileMenuOpen={isMobileMenuOpen}
          onMobileClose={() => setIsMobileMenuOpen(false)}
        />

        {isMobileMenuOpen ? (
          <button
            type="button"
            aria-label="Close sidebar overlay"
            className="fixed inset-0 z-[60] bg-slate-950/35 backdrop-blur-sm lg:hidden"
            onClick={() => setIsMobileMenuOpen(false)}
          />
        ) : null}

        <div className="flex min-w-0 flex-1 flex-col">
          <div className="px-4 pt-4 md:px-6 md:pt-6 lg:px-8">
            <AdminHeader onMenuClick={() => setIsMobileMenuOpen(true)} />
          </div>

          <main className="w-full flex-1 p-4 md:p-6 lg:p-8">{children}</main>
        </div>
      </div>
    </div>
  );
}
