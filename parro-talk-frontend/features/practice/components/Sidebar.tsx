import { Mic, Play, Book, FileText, Star, LogOut, PanelLeftClose, PanelLeftOpen, CloudUpload, Shield } from "lucide-react";

import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";

interface SidebarProps {
  isCollapsed: boolean;
  onToggle: () => void;
  isMobileMenuOpen?: boolean;
  onMobileClose?: () => void;
}

export default function Sidebar({ isCollapsed, onToggle, isMobileMenuOpen, onMobileClose }: SidebarProps) {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  const menuItems = [
    { icon: Play, label: "Library", path: "/library", active: pathname === "/library" },
    { icon: Book, label: "Dictionary", path: "#" },
    { icon: FileText, label: "Notes", path: "#" },
    { icon: Mic, label: "Transcript", path: "#" },
        ...(user?.role === "ADMIN" ? [
      { icon: Shield, label: "Admin", path: "/admin/lessons", active: pathname.startsWith("/admin") },
      { icon: CloudUpload, label: "Upload", path: "/upload", active: pathname.startsWith("/upload") }
    ] : []),

  ];

  return (
    <aside
      className={`
        bg-white border-r border-gray-100 flex flex-col h-screen top-0 z-[70] transition-all duration-300
        
        /* MOBILE & TABLET (< lg) */
        fixed inset-y-0 left-0 shadow-2xl lg:shadow-none
        w-[280px] sm:w-[320px]
        ${isMobileMenuOpen ? "translate-x-0" : "-translate-x-full"}
        
        /* DESKTOP (>= lg) */
        lg:relative lg:sticky lg:translate-x-0
        ${isCollapsed ? "lg:w-20" : "lg:w-64"}
      `}



    >

      <div className={`p-6 ${isCollapsed ? "flex flex-col items-center" : ""}`}>
        <div
          className={`flex mb-10 ${isCollapsed
            ? "flex-col items-center gap-4"
            : "items-center justify-between"
            }`}
        >
          {/* Logo */}
          <Link href="/" className="flex items-center justify-center group outline-none">
            {isCollapsed ? (
              <div className="relative w-14 h-14">
                <Image
                  src="/logo.png"
                  alt="ParroTalk Logo"
                  fill
                  sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                  priority
                  className="object-contain"
                />
              </div>
            ) : (
              <div className="relative w-45 h-16">
                <Image
                  src="/logo_long.png"
                  alt="ParroTalk Logo"
                  fill
                  sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                  priority
                  className="object-contain object-left"
                />
              </div>
            )}
          </Link>

          {/* Toggle button - Desktop Only */}
          <button
            onClick={onToggle}
            className="hidden lg:flex p-2 hover:bg-gray-100 rounded-xl text-gray-400 transition-all active:scale-90 z-10"
          >
            {isCollapsed ? <PanelLeftOpen size={20} /> : <PanelLeftClose size={20} />}
          </button>

          {/* Close button - Mobile Only */}
          <button
            onClick={onMobileClose}
            className="lg:hidden p-2 hover:bg-gray-100 rounded-xl text-gray-400 transition-all active:scale-90 z-10"
          >
            <PanelLeftClose size={20} />
          </button>

        </div>

        {/* Level Indicator */}
        <div className={`bg-green-50 rounded-2xl p-3 flex items-center gap-4 mb-8 border border-green-100/50 transition-all overflow-hidden ${isCollapsed ? "w-12 h-12 justify-center" : ""}`}>
          <div className="bg-green-500 text-white font-black w-8 h-8 rounded-lg flex items-center justify-center text-[10px] shadow-sm shadow-green-200 shrink-0">
            B2
          </div>
          {!isCollapsed && (
            <div className="transition-all whitespace-nowrap">
              <div className="text-[10px] font-black text-green-700 uppercase tracking-wider">Level B2</div>
              <div className="text-[9px] text-green-600 font-bold opacity-70">Upper Intermediate</div>
            </div>
          )}
        </div>

        {/* Menu */}
        <nav className={`flex flex-col gap-2 ${isCollapsed ? "items-center" : ""}`}>
          {menuItems.map((item, index) => (
            <Link
              key={index}
              href={item.path}
              title={isCollapsed ? item.label : ""}
              className={`
                flex items-center gap-4 px-3 py-2.5 rounded-xl font-bold transition-all relative group
                ${item.active
                  ? "bg-green-500 text-white shadow-lg shadow-green-100"
                  : "text-gray-400 hover:text-green-500 hover:bg-green-50"}
                ${isCollapsed ? "w-10 justify-center p-0 h-10" : ""}
              `}
            >
              <item.icon size={20} className="shrink-0" />
              {!isCollapsed && <span className="text-sm">{item.label}</span>}
            </Link>
          ))}
        </nav>
      </div>

      <div className={`mt-auto p-6 flex flex-col gap-4 ${isCollapsed ? "items-center" : ""}`}>
        <button className={`py-4 bg-orange-50 text-orange-600 rounded-2xl font-bold transition-all flex items-center justify-center gap-2 border border-orange-100 overflow-hidden ${isCollapsed ? "w-10 h-10 p-0" : "w-full text-sm"}`}>
          <Star size={16} fill="currentColor" className="shrink-0" />
          {!isCollapsed && "Unlock Pro"}
        </button>

        <button
          onClick={logout}
          className={`flex items-center gap-4 text-gray-400 font-bold hover:text-red-500 transition-all ${isCollapsed ? "w-10 h-10 justify-center pr-0" : "px-4 py-2 text-sm"}`}
        >
          <LogOut size={20} className="shrink-0" />
          {!isCollapsed && "Logout"}
        </button>
      </div>
    </aside>
  );
}

