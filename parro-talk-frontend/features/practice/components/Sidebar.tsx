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
    { icon: Play, label: "Library", path: "/library", active: pathname === "/library", hidden: false },
    { icon: Book, label: "Dictionary", path: "#", hidden: true },
    { icon: FileText, label: "Notes", path: "#", hidden: true },
    { icon: Mic, label: "Transcript", path: "#", hidden: true },
    ...(user?.role === "ADMIN" ? [
      { icon: Shield, label: "Admin", path: "/admin/lessons", active: pathname.startsWith("/admin"), hidden: false },
      { icon: CloudUpload, label: "Upload", path: "/upload", active: pathname.startsWith("/upload"), hidden: false }
    ] : user?.role === "PRO_USER" ? [
      { icon: CloudUpload, label: "Upload", path: "/upload", active: pathname.startsWith("/upload"), hidden: false }
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

        {/* Menu */}
        <nav className={`flex flex-col gap-2 ${isCollapsed ? "items-center" : ""}`}>
          {menuItems.map((item, index) => (
            item.hidden
              ? null
              : (
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
              )
          ))}
        </nav>
      </div>

      <div className={`mt-auto p-6 flex flex-col gap-4 ${isCollapsed ? "items-center" : ""}`}>
        {user?.role === "USER" && (
          <button className={`py-4 bg-orange-50 text-orange-600 rounded-2xl font-bold transition-all flex items-center justify-center gap-2 border border-orange-100 overflow-hidden ${isCollapsed ? "w-10 h-10 p-0" : "w-full text-sm"}`}>
            <Star size={16} fill="currentColor" className="shrink-0" />
            {!isCollapsed && "Upgrade to Pro"}
          </button>
        )}

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

