"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSidebar } from "@/contexts/SidebarContext";
import { useGlass } from "./ThemeProvider";
import { useTheme } from "next-themes";

const navItems = [
  { href: "/dashboard", label: "Dashboard", icon: "📊" },
  { href: "/clientes", label: "Carteira", icon: "👥" },
  { href: "/configuracoes", label: "Config", icon: "⚙️" },
];

export default function Sidebar() {
  const pathname = usePathname();
  const { isCollapsed, setIsCollapsed } = useSidebar();
  const { isGlass, toggleGlass } = useGlass();
  const { theme, setTheme } = useTheme();

  return (
    <aside
      className={`glass-sidebar fixed left-0 top-0 h-full z-40 transition-all duration-300 flex flex-col ${
        isCollapsed ? "w-20" : "w-64"
      }`}
    >
      <div className="flex items-center justify-between p-4 border-b border-white/5">
        {!isCollapsed && (
          <span className="text-xl font-bold text-[var(--primary)]">IORI.Tv</span>
        )}
        <button onClick={() => setIsCollapsed(!isCollapsed)} className="p-2 rounded-lg hover:bg-white/5 text-[var(--on-surface-variant)] transition-colors">
          {isCollapsed ? "→" : "←"}
        </button>
      </div>

      <nav className="flex-1 p-3 space-y-1">
        {navItems.map((item) => {
          const isActive = pathname.startsWith(item.href);
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 ${
                isActive
                  ? "bg-[var(--primary)]/10 text-[var(--primary)] font-semibold"
                  : "text-[var(--on-surface-variant)] hover:bg-white/5 hover:text-[var(--on-surface)]"
              }`}
            >
              <span className="text-xl">{item.icon}</span>
              {!isCollapsed && <span className="text-sm">{item.label}</span>}
            </Link>
          );
        })}
      </nav>

      <div className="p-3 border-t border-white/5 space-y-2">
        <button
          onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-[var(--on-surface-variant)] hover:bg-white/5 transition-all text-sm"
        >
          <span>{theme === "dark" ? "☀️" : "🌙"}</span>
          {!isCollapsed && <span>{theme === "dark" ? "Tema Claro" : "Tema Escuro"}</span>}
        </button>

        <button
          onClick={toggleGlass}
          className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all text-sm ${
            isGlass
              ? "bg-[var(--primary)]/10 text-[var(--primary)] font-semibold"
              : "text-[var(--on-surface-variant)] hover:bg-white/5"
          }`}
        >
          <span>💎</span>
          {!isCollapsed && <span>Skin Glass {isGlass ? "ON" : "OFF"}</span>}
        </button>
      </div>
    </aside>
  );
}
