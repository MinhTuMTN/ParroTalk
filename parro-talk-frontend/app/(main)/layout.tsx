"use client";

import React, { useState } from "react";
import Sidebar from "@/components/practice/Sidebar";
import { UIProvider, useUI } from "@/context/UIContext";

function LayoutContent({ children }: { children: React.ReactNode }) {
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const { isMobileMenuOpen, closeMobileMenu } = useUI();

  return (
    <div className="flex min-h-screen bg-[#FDFDFD] font-sans selection:bg-green-100 relative">
      <Sidebar
        isCollapsed={isSidebarCollapsed}
        onToggle={() => setIsSidebarCollapsed(!isSidebarCollapsed)}
        isMobileMenuOpen={isMobileMenuOpen}
        onMobileClose={closeMobileMenu}
      />

      {/* Overlay for mobile */}
      {isMobileMenuOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-[60] lg:hidden backdrop-blur-sm transition-opacity"
          onClick={closeMobileMenu}
        />
      )}

      <div className="flex-1 flex flex-col min-w-0 h-screen overflow-y-auto">
        {children}
      </div>
    </div>
  );
}

export default function MainLayout({ children }: { children: React.ReactNode }) {
  return (
    <UIProvider>
      <LayoutContent>{children}</LayoutContent>
    </UIProvider>
  );
}


