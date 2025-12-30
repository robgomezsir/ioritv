"use client";

import { ThemeProvider as NextThemesProvider } from "next-themes";
import { useEffect, useState } from "react";

export function ThemeProvider({ children }: { children: React.ReactNode }) {
    const [mounted, setMounted] = useState(false);

    useEffect(() => {
        const timer = setTimeout(() => setMounted(true), 0);
        return () => clearTimeout(timer);
    }, []);

    if (!mounted) {
        return <>{children}</>;
    }

    return (
        <NextThemesProvider attribute="class" defaultTheme="dark" enableSystem={false}>
            {children}
        </NextThemesProvider>
    );
}
