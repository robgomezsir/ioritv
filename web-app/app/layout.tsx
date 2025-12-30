"use client";

import { Geist, Geist_Mono } from "next/font/google";
import Sidebar from "@/components/Sidebar";
import "./globals.css";
import { ThemeProvider } from "@/components/ThemeProvider";
import { usePathname } from "next/navigation";
import { SidebarProvider, useSidebar } from "@/contexts/SidebarContext";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

function LayoutContent({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const isLoginPage = pathname === "/login";
  const { isCollapsed } = useSidebar();

  return (
    <body
      className={`${geistSans.variable} ${geistMono.variable} antialiased bg-gray-50 dark:bg-gray-950 text-gray-900 dark:text-gray-100 ${!isLoginPage ? 'flex min-h-screen' : ''} transition-colors duration-300`}
    >
      <ThemeProvider>
        {!isLoginPage && <Sidebar />}
        <div className={`flex-1 flex flex-col min-h-screen relative transition-all duration-300 ${!isLoginPage ? (isCollapsed ? 'ml-0 md:ml-20' : 'ml-0 md:ml-64') : ''}`}>
          {children}
        </div>
      </ThemeProvider>
    </body>
  );
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <SidebarProvider>
        <LayoutContent>{children}</LayoutContent>
      </SidebarProvider>
    </html>
  );
}
