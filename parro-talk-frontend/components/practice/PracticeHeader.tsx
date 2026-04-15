import { Settings, HelpCircle, Menu, X } from "lucide-react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { useUI } from "@/context/UIContext";

interface PracticeHeaderProps {
  currentSentence: number;
  totalSentences: number;
  percent: number;
  onFinish?: () => void;
  isAllCompleted?: boolean;
}

export default function PracticeHeader({ currentSentence, totalSentences, percent, onFinish, isAllCompleted }: PracticeHeaderProps) {
  const { user } = useAuth();
  const { openMobileMenu } = useUI();

  return (
    <header className="h-16 bg-white border-b border-gray-100 px-4 md:px-6 flex items-center justify-between sticky top-0 z-50 backdrop-blur-md bg-white/90">
      <div className="flex items-center gap-4">
        <button
          onClick={openMobileMenu}
          className="lg:hidden p-2 hover:bg-gray-100 rounded-xl text-gray-400 transition-all active:scale-95"
        >
          <Menu size={24} />
        </button>

        <Link href="/library" className="hidden sm:flex items-center gap-2 group">
          <div className="w-8 h-8 bg-green-500 rounded-lg flex items-center justify-center text-white shadow-lg shadow-green-100 group-hover:scale-110 transition-transform">
            <span className="font-black text-xs">P</span>
          </div>
          <span className="font-black text-sm text-gray-800 tracking-tight">ParroTalk</span>
        </Link>
      </div>

      <div className="flex-1 max-w-md px-4 sm:px-10">
        <div className="flex items-center justify-between mb-1.5 px-0.5">
          <span className="text-[9px] font-black text-gray-400 uppercase tracking-widest flex items-center gap-2">
            <span className="hidden xs:inline">Lesson</span> Progress
            <span className="text-gray-300">•</span>
            <span className="text-gray-600">Sentence {currentSentence}/{totalSentences}</span>
          </span>
          <span className="text-[9px] font-black text-green-500 uppercase tracking-widest bg-green-50 px-1.5 py-0.5 rounded">{percent}%</span>
        </div>
        <div className="h-1.5 w-full bg-gray-100 rounded-full overflow-hidden shadow-inner">
          <div className="h-full bg-green-500 transition-all duration-1000 ease-out shadow-[0_0_8px_rgba(34,197,94,0.4)]" style={{ width: `${percent}%` }} />
        </div>
      </div>

      <div className="flex items-center gap-1 sm:gap-3">
        {isAllCompleted && (
          <button
            onClick={onFinish}
            className="hidden xs:flex items-center gap-2 px-4 py-2 bg-green-500 text-white rounded-xl text-xs font-black uppercase tracking-widest hover:bg-green-600 transition-all shadow-lg shadow-green-100 animate-bounce-subtle"
          >
            Finish
          </button>
        )}
        <button className="hidden sm:flex p-2 text-gray-400 hover:text-gray-800 hover:bg-gray-50 rounded-xl transition-all">
          <Settings size={18} />
        </button>
        <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-green-100 text-green-600 flex items-center justify-center font-black text-xs border-2 border-white shadow-sm ml-1 uppercase cursor-pointer hover:bg-green-200 transition-colors">
          {user?.fullName?.charAt(0) || "U"}
        </div>
      </div>
    </header>
  );
}
