"use client";

import { useEffect } from "react";
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

  useEffect(() => {
    // Handle ChunkLoadError globally
    const handleError = (e: ErrorEvent) => {
      if (e.message.includes('Loading chunk') || e.message.includes('CSS chunk')) {
        console.warn('ChunkLoadError detected, forcing reload...', e);
        window.location.reload();
      }
    };

    window.addEventListener('error', handleError);

    if ('serviceWorker' in navigator && window.location.protocol === 'https:') {
      window.addEventListener('load', () => {
        navigator.serviceWorker.register('/sw.js').then(
          (registration) => {
            console.log('SW registered: ', registration);
            // Check for updates
            registration.onupdatefound = () => {
              const installingWorker = registration.installing;
              if (installingWorker) {
                installingWorker.onstatechange = () => {
                  if (installingWorker.state === 'installed') {
                    if (navigator.serviceWorker.controller) {
                      console.log('New content available; please refresh.');
                      window.location.reload();
                    }
                  }
                };
              }
            };
          },
          (registrationError) => {
            console.log('SW registration failed: ', registrationError);
          }
        );
      });
    }

    return () => window.removeEventListener('error', handleError);
  }, []);

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
      <head>
        <link rel="manifest" href="/manifest.json" />
        <meta name="theme-color" content="#141b2d" />
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
