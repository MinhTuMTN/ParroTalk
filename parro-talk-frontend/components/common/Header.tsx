import { Bell, CircleHelp, Menu, Settings } from "lucide-react";
import Input from "@/components/ui/Input";

type HeaderProps = {
  search: string;
  onSearchChange: (value: string) => void;
  onMenuClick: () => void;
};

export default function Header({
  search,
  onSearchChange,
  onMenuClick,
}: HeaderProps) {
  return (
    <header className="flex flex-col gap-4 rounded-[28px] bg-white/70 p-4 shadow-sm md:flex-row md:items-center md:justify-between">
      <div className="flex flex-1 items-center gap-4">
        <button
          onClick={onMenuClick}
          className="rounded-xl border border-slate-200 p-2 text-slate-600 md:hidden"
          aria-label="Open sidebar"
        >
          <Menu className="h-5 w-5" />
        </button>
        <h2 className="min-w-fit text-2xl font-bold text-slate-900">Lesson Manager</h2>
        <div className="max-w-xl flex-1">
          <Input
            withSearchIcon
            value={search}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Search lessons..."
          />
        </div>
      </div>
      <div className="flex items-center gap-4 text-slate-500">
        <button className="rounded-full bg-slate-200/70 p-2.5">
          <Bell className="h-5 w-5" />
        </button>
        <Settings className="h-5 w-5" />
        <CircleHelp className="h-5 w-5" />
        <div className="h-9 w-9 rounded-full bg-emerald-200" />
      </div>
    </header>
  );
}

