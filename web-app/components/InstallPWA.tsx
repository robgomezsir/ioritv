"use client";

import { useEffect, useState } from "react";
import { ArrowDownTrayIcon } from "@heroicons/react/24/outline";

interface BeforeInstallPromptEvent extends Event {
    prompt(): Promise<void>;
    userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

export default function InstallPWA({ isCollapsed }: { isCollapsed: boolean }) {
    const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
    const [isVisible, setIsVisible] = useState(false);

    useEffect(() => {
        const handler = (e: Event) => {
            e.preventDefault();
            setDeferredPrompt(e as BeforeInstallPromptEvent);
            setIsVisible(true);
        };

        window.addEventListener("beforeinstallprompt", handler as EventListener);

        // Check if app is already installed
        if (window.matchMedia("(display-mode: standalone)").matches) {
            setIsVisible(false);
        }

        return () => {
            window.removeEventListener("beforeinstallprompt", handler);
        };
    }, []);

    const handleInstallClick = async () => {
        if (!deferredPrompt) return;

        // Show the install prompt
        deferredPrompt.prompt();

        // Wait for the user to respond to the prompt
        const { outcome } = await deferredPrompt.userChoice;
        console.log(`User response to the install prompt: ${outcome}`);

        // We've used the prompt, and can't use it again, throw it away
        setDeferredPrompt(null);
        setIsVisible(false);
    };

    if (!isVisible) return null;

    return (
        <button
            onClick={handleInstallClick}
            className={`flex items-center gap-3 w-full px-4 py-3 text-blue-400 hover:bg-white/5 transition-all duration-200 rounded-xl group mt-auto mb-2 border border-blue-500/20 bg-blue-500/5`}
            title="Instalar Aplicativo"
        >
            <div className="flex items-center justify-center w-6 h-6 shrink-0 group-hover:scale-110 transition-transform duration-200">
                <ArrowDownTrayIcon className="w-6 h-6" />
            </div>
            {!isCollapsed && (
                <span className="font-medium text-sm whitespace-nowrap overflow-hidden">
                    Instalar App
                </span>
            )}
        </button>
    );
}
