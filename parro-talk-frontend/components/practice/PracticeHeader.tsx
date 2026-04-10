"use client";

import { Settings, HelpCircle, User } from "lucide-react";
import Link from "next/link";

export default function PracticeHeader() {
  return (
    <header className="h-16 bg-white border-b border-gray-100 px-6 flex items-center justify-between sticky top-0 z-10">
      <div className="flex items-center gap-8">
        {/* Navigation moved to Sidebar */}
      </div>

      <div className="flex-1 max-w-md px-10">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-[9px] font-black text-gray-400 uppercase tracking-widest">Sentence 3/20</span>
            <span className="text-[9px] font-black text-green-500 uppercase tracking-widest">15%</span>
          </div>
          <div className="h-1 w-full bg-gray-100 rounded-full overflow-hidden">
            <div className="h-full bg-green-500 w-[15%] transition-all duration-1000" />
          </div>
      </div>

      <div className="flex items-center gap-3">
          <button className="p-2 text-gray-400 hover:text-gray-800 hover:bg-gray-50 rounded-xl transition-all">
              <Settings size={18} />
          </button>
          <button className="p-2 text-gray-400 hover:text-gray-800 hover:bg-gray-50 rounded-xl transition-all">
              <HelpCircle size={18} />
          </button>
          <div className="w-8 h-8 rounded-lg bg-gray-100 overflow-hidden border-2 border-white shadow-sm ml-1">
              <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" alt="User" />
          </div>
      </div>
    </header>
  );
}
