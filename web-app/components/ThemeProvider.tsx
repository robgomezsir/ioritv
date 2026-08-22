"use client";

import { ThemeProvider as NextThemesProvider } from "next-themes";
import { useEffect, useState, createContext, useContext } from "react";

interface GlassContextType {
  isGlass: boolean;
  toggleGlass: () => void;
}

export const GlassContext = createContext<GlassContextType>({
  isGlass: false,
  toggleGlass: () => {},
});

export const useGlass = () => useContext(GlassContext);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [mounted, setMounted] = useState(false);
  const [isGlass, setIsGlass] = useState(false);

  useEffect(() => {
    const saved = localStorage.getItem("glass_enabled");
    if (saved === "true") setIsGlass(true);
    const timer = setTimeout(() => setMounted(true), 0);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (isGlass) {
      document.documentElement.classList.add("glass");
    } else {
      document.documentElement.classList.remove("glass");
    }
    localStorage.setItem("glass_enabled", isGlass.toString());
  }, [isGlass]);

  const toggleGlass = () => setIsGlass((prev) => !prev);

  if (!mounted) {
    return <>{children}</>;
  }

  return (
    <GlassContext.Provider value={{ isGlass, toggleGlass }}>
      <NextThemesProvider attribute="class" defaultTheme="dark" enableSystem={false}>
        {children}
      </NextThemesProvider>
    </GlassContext.Provider>
  );
}
