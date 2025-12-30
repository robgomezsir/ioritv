"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import Image from "next/image";
import { useSidebar } from "@/contexts/SidebarContext";
import InstallPWA from "./InstallPWA";

export default function Sidebar() {
    const pathname = usePathname();
    const [isOpen, setIsOpen] = useState(false);
    const { isCollapsed, setIsCollapsed } = useSidebar();

    // Do not show sidebar on login page
    if (pathname === "/login") return null;

    const menuItems = [
        { name: "Dashboard", path: "/dashboard", icon: "/ranking_claro.png" },
        { name: "Clientes", path: "/clientes", icon: "/carteira_claro.png" },
        { name: "Configurações", path: "/configuracoes", icon: "⚙️" },
    ];

    return (
        <>
            {/* Mobile Menu Button */}
            <button
                className="md:hidden fixed top-4 left-4 z-50 p-2 bg-white dark:bg-gray-800 rounded-lg text-gray-900 dark:text-white shadow-lg border border-gray-200 dark:border-gray-700"
                onClick={() => setIsOpen(!isOpen)}
            >
                {isOpen ? "✕" : "☰"}
            </button>

            {/* Sidebar Container */}
            <aside className={`
                fixed inset-y-0 left-0 z-40
                ${isCollapsed ? 'w-20' : 'w-64'} 
                bg-gradient-to-b from-[#141b2d] to-[#0b1224] border-r border-[#1e293b]
                transform transition-all duration-300 ease-in-out
                ${isOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"}
            `}>
                <div className="flex flex-col h-full overflow-hidden">
                    {/* Logo / Header - Now Clickable */}
                    <button
                        onClick={() => setIsCollapsed(!isCollapsed)}
                        className="h-16 flex items-center justify-center border-b border-[#1e293b] flex-shrink-0 px-4 hover:bg-white/5 transition-colors duration-200 group cursor-pointer"
                        title={isCollapsed ? "Expandir sidebar" : "Contrair sidebar"}
                    >
                        <div className="relative">
                            {isCollapsed ? (
                                <Image
                                    src="/logom.png"
                                    alt="IoriTV"
                                    width={40}
                                    height={40}
                                    className="object-contain transition-transform duration-200 group-hover:scale-110"
                                    priority
                                />
                            ) : (
                                <Image
                                    src="/logo.png"
                                    alt="IoriTV Logo"
                                    width={120}
                                    height={40}
                                    className="object-contain transition-transform duration-200 group-hover:scale-105"
                                    priority
                                />
                            )}
                            {/* Subtle indicator */}
                            <div className={`absolute -right-1 -bottom-1 w-3 h-3 bg-blue-500 rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-200 ${isCollapsed ? 'animate-pulse' : ''}`} />
                        </div>
                    </button>

                    {/* Navigation Links */}
                    <nav className="flex-1 p-4 space-y-2 overflow-y-auto">
                        {menuItems.map((item) => {
                            const isActive = pathname.startsWith(item.path);
                            return (
                                <Link
                                    key={item.path}
                                    href={item.path}
                                    className={`
                                        flex items-center ${isCollapsed ? 'justify-center' : 'gap-3'} px-4 py-3 rounded-lg transition-all duration-200
                                        ${isActive
                                            ? "bg-blue-600/20 text-blue-400 border border-blue-500/30 shadow-lg shadow-black/20"
                                            : "text-gray-400 hover:bg-white/5 hover:text-white"
                                        }
                                    `}
                                    onClick={() => setIsOpen(false)}
                                    title={isCollapsed ? item.name : undefined}
                                >
                                    <span className="flex items-center justify-center w-6 h-6">
                                        {item.icon.startsWith('/') ? (
                                            <Image
                                                src={item.icon}
                                                alt=""
                                                width={24}
                                                height={24}
                                                className={`object-contain transition-all duration-200 ${isActive ? 'brightness-125' : 'opacity-70 group-hover:opacity-100'}`}
                                            />
                                        ) : (
                                            <span className="text-lg">{item.icon}</span>
                                        )}
                                    </span>
                                    {!isCollapsed && <span className="font-medium">{item.name}</span>}
                                </Link>
                            );
                        })}
                    </nav>

                    {/* Bottom Section: Install Button & Version */}
                    <div className="p-4 border-t border-[#1e293b] space-y-2">
                        <InstallPWA isCollapsed={isCollapsed} />
                        {!isCollapsed && (
                            <div className="text-[10px] text-center text-gray-500 font-medium tracking-wider uppercase opacity-50">
                                v4.2.0 • PWA Ready
                            </div>
                        )}
                    </div>
                </div>
            </aside>

            {/* Overlay for mobile */}
            {isOpen && (
                <div
                    className="fixed inset-0 bg-black/50 z-30 md:hidden backdrop-blur-sm"
                    onClick={() => setIsOpen(false)}
                />
            )}
        </>
    );
}
