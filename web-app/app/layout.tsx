"use client";

import { Geist, Geist_Mono } from "next/font/google";
import Sidebar from "@/components/Sidebar";
import "./globals.css";
import { ThemeProvider, useGlass } from "@/components/ThemeProvider";
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

function AuroraBackground() {
  const { isGlass } = useGlass();
  if (!isGlass) return null;
  return <div className="aurora-bg" />;
}

function LayoutContent({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const isLoginPage = pathname === "/login";
  const { isCollapsed } = useSidebar();

  return (
    <body
      className={`${geistSans.variable} ${geistMono.variable} antialiased bg-[var(--background)] text-[var(--foreground)] ${!isLoginPage ? "flex min-h-screen" : ""} transition-colors duration-300`}
    >
      <ThemeProvider>
        <AuroraBackground />
        {!isLoginPage && <Sidebar />}
        <div
          className={`flex-1 flex flex-col min-h-screen relative transition-all duration-300 ${
            !isLoginPage ? (isCollapsed ? "ml-0 md:ml-20" : "ml-0 md:ml-64") : ""
          }`}
        >
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
      <head>
        <link rel="manifest" href="/manifest.json" />
        <meta name="theme-color" content="#0E1415" />
        <meta name="mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-status-bar-style" content="default" />
        <meta name="apple-mobile-web-app-title" content="IoriTV" />
        <link rel="apple-touch-icon" href="/logom.png" />
      </head>
      <SidebarProvider>
        <LayoutContent>{children}</LayoutContent>
      </SidebarProvider>
    </html>
  );
}
