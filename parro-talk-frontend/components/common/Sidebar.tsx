"use client";

import {
  BarChart3,
  Boxes,
  FileText,
  LayoutDashboard,
  LogOut,
  PanelLeftClose,
  PanelLeftOpen,
  Settings,
  Tags,
  UsersRound,
} from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";

type SidebarProps = {
  collapsed: boolean;
  onToggle: () => void;
  isMobileMenuOpen?: boolean;
  onMobileClose?: () => void;
};

type SidebarItem = {
  icon: typeof LayoutDashboard;
  label: string;
  path?: string;
  active?: boolean;
  disabled?: boolean;
};

export default function Sidebar({
  collapsed,
  onToggle,
  isMobileMenuOpen,
  onMobileClose,
}: SidebarProps) {
  const pathname = usePathname();
  const { logout } = useAuth();
  const isMobileOpen = isMobileMenuOpen ?? !collapsed;

  const menuItems: SidebarItem[] = [
    {
      icon: LayoutDashboard,
      label: "Dashboard",
      disabled: true,
    },
    {
      icon: FileText,
      label: "Lessons",
      path: "/admin/lessons",
      active: pathname.startsWith("/admin/lessons"),
    },
    {
      icon: UsersRound,
      label: "Users",
      path: "/admin/users",
      active: pathname.startsWith("/admin/users"),
    },
    {
      icon: Boxes,
      label: "Categories",
      disabled: true,
    },
    {
      icon: Tags,
      label: "Tags",
      disabled: true,
    },
    {
      icon: BarChart3,
      label: "Reports",
      disabled: true,
    },
    {
      icon: Settings,
      label: "Settings",
      disabled: true,
    },
  ];

  return (
    <aside
      className={`fixed inset-y-0 left-0 top-0 z-[70] flex h-screen flex-col border-r border-gray-100 bg-white shadow-2xl transition-all duration-300 lg:sticky lg:shadow-none ${isMobileOpen ? "translate-x-0" : "-translate-x-full"
        } ${collapsed ? "lg:w-20" : "lg:w-64"
        } w-[280px] sm:w-[320px] lg:translate-x-0`
      }
    >
      <div className={`p-6 ${collapsed ? "flex flex-col items-center" : ""}`}>
        <div
          className={`mb-10 flex ${collapsed ? "flex-col items-center gap-4" : "items-center justify-between"
            }`}
        >
          <Link href="/admin/lessons" className="outline-none">
            {collapsed ? (
              <div className="relative h-14 w-14">
                <Image
                  src="/logo.png"
                  alt="ParroTalk Logo"
                  fill
                  sizes="56px"
                  priority
                  className="object-contain"
                />
              </div>
            ) : (
              <div className="relative h-16 w-44">
                <Image
                  src="/logo_long.png"
                  alt="ParroTalk Logo"
                  fill
                  sizes="176px"
                  priority
                  className="object-contain object-left"
                />
              </div>
            )}
          </Link>

          <button
            onClick={onToggle}
            className="hidden rounded-xl p-2 text-gray-400 transition-all hover:bg-gray-100 active:scale-90 lg:flex"
            aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          >
            {collapsed ? <PanelLeftOpen size={20} /> : <PanelLeftClose size={20} />}
          </button>

          <button
            onClick={onMobileClose ?? onToggle}
            className="rounded-xl p-2 text-gray-400 transition-all hover:bg-gray-100 active:scale-90 lg:hidden"
            aria-label="Close sidebar"
          >
            <PanelLeftClose size={20} />
          </button>
        </div>

        <nav className={`flex flex-col gap-2 ${collapsed ? "items-center" : ""}`}>
          {menuItems.map((item) =>
            item.path ? (
              <Link
                key={item.label}
                href={item.path}
                title={collapsed ? item.label : ""}
                onClick={onMobileClose}
                className={`group relative flex items-center gap-4 rounded-xl px-3 py-2.5 font-bold transition-all ${
                  item.active
                    ? "bg-emerald-50 text-emerald-700"
                    : "text-slate-500 hover:bg-emerald-50 hover:text-emerald-700"
                } ${collapsed ? "h-10 w-10 justify-center p-0" : ""}`}
              >
                <item.icon size={20} className="shrink-0" />
                {!collapsed && <span className="text-sm">{item.label}</span>}
              </Link>
            ) : (
              <button
                key={item.label}
                type="button"
                disabled={item.disabled}
                title={collapsed ? item.label : ""}
                className={`flex items-center gap-4 rounded-xl px-3 py-2.5 font-bold text-slate-400 ${
                  collapsed ? "h-10 w-10 justify-center p-0" : ""
                }`}
              >
                <item.icon size={20} className="shrink-0" />
                {!collapsed && <span className="text-sm">{item.label}</span>}
              </button>
            ),
          )}
        </nav>
      </div>

      <div className={`mt-auto flex flex-col gap-4 p-6 ${collapsed ? "items-center" : ""}`}>
        <button
          type="button"
          onClick={logout}
          className={`
            flex items-center gap-4 font-bold text-gray-400 transition-all hover:text-red-500 ${collapsed ? "h-10 w-10 justify-center" : "px-4 py-2 text-sm"
            }`}
        >
          <LogOut size={20} className="shrink-0" />
          {!collapsed && "Logout"}
        </button>
      </div>
    </aside>
  );
}

