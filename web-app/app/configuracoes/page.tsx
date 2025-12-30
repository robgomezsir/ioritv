"use client";

import { useEffect, useState } from "react";
import { onAuthStateChanged } from "firebase/auth";
import { collection, getDocs, query, orderBy } from "firebase/firestore";
import { auth, db } from "@/firebase/config";
import { useRouter } from "next/navigation";
import { useTheme } from "next-themes";
import { User } from "firebase/auth";
import { ArrowDownTrayIcon } from "@heroicons/react/24/outline";

function ThemeToggle() {
    const { theme, setTheme } = useTheme();
    const [mounted, setMounted] = useState(false);

    useEffect(() => {
        const timer = setTimeout(() => setMounted(true), 0);
        return () => clearTimeout(timer);
    }, []);

    if (!mounted) return null;

    return (
        <div className="flex gap-1">
            <button
                onClick={() => setTheme("light")}
                className={`px-3 py-1.5 rounded-md text-sm font-medium transition-all ${theme === "light"
                    ? "bg-white text-gray-900 shadow-sm"
                    : "text-gray-500 hover:text-gray-900 dark:hover:text-gray-300"
                    }`}
            >
                ☀️ Claro
            </button>
            <button
                onClick={() => setTheme("dark")}
                className={`px-3 py-1.5 rounded-md text-sm font-medium transition-all ${theme === "dark"
                    ? "bg-gray-700 text-white shadow-sm"
                    : "text-gray-500 hover:text-gray-900 dark:hover:text-gray-300"
                    }`}
            >
                🌙 Escuro
            </button>
        </div>
    );
}

export default function ConfiguracoesPage() {
    const [user, setUser] = useState<User | null>(null);
    const router = useRouter();

    useEffect(() => {
        const unsubscribe = onAuthStateChanged(auth, (authUser) => {
            if (!authUser) {
                router.push("/login");
            } else {
                setUser(authUser);
            }
        });
        return () => unsubscribe();
    }, [router]);

    const handleExportCSV = async () => {
        try {
            const q = query(collection(db, "clientes"), orderBy("NOME"));
            const snapshot = await getDocs(q);
            const data = snapshot.docs.map(doc => doc.data());

            const headers = [
                "Nome", "Usuário", "Senha", "Situação", "Vencimento", "Créditos",
                "WhatsApp", "Valor", "Custo", "Desconto", "Servidor", "MAC",
                "Modelo", "Data de Início"
            ];

            const rows = data.map(item => [
                item.NOME || "",
                item.USUARIO || "",
                item.SENHA || "",
                item.SITUACAO || "",
                item.VENCIMENTO || "",
                item.CREDITOS || 0,
                item.WHATSAPP || "",
                item.VALOR || 0,
                item.CUSTO || 0,
                item.DESCONTO || 0,
                item.SERVIDOR || "",
                item.MAC || "",
                item.MODELO || "",
                item.INICIO ? (item.INICIO.toDate ? item.INICIO.toDate().toLocaleDateString('pt-BR') : "") : ""
            ]);

            const csvContent = [
                headers.join(","),
                ...rows.map(row => row.map(val => `"${String(val).replace(/"/g, '""')}"`).join(","))
            ].join("\n");

            const blob = new Blob(["\uFEFF" + csvContent], { type: 'text/csv;charset=utf-8;' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.setAttribute("href", url);
            link.setAttribute("download", `clientes_ioritv_${new Date().toISOString().split('T')[0]}.csv`);
            link.style.visibility = 'hidden';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        } catch (error) {
            console.error("Erro ao exportar CSV:", error);
            alert("Erro ao exportar clientes.");
        }
    };

    if (!user) return <div className="p-8 text-center text-white">Carregando...</div>;

    return (
        <div className="p-6 max-w-2xl mx-auto space-y-6">
            <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent mb-6">
                Configurações
            </h1>

            <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-8 shadow-lg">
                <div className="flex items-center gap-6">
                    <div className="w-24 h-24 bg-gray-100 dark:bg-gray-700 rounded-full flex items-center justify-center text-4xl border-4 border-gray-200 dark:border-gray-600">
                        {user.photoURL ? (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img src={user.photoURL} alt="Perfil" className="w-full h-full rounded-full object-cover" />
                        ) : (
                            <span>👤</span>
                        )}
                    </div>
                    <div>
                        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">{user.displayName || "Administrador"}</h2>
                        <p className="text-gray-500 dark:text-gray-400">{user.email}</p>
                        <p className="text-xs text-gray-400 dark:text-gray-500 mt-2">UID: {user.uid}</p>
                    </div>
                </div>

                <div className="mt-8 pt-8 border-t border-gray-700">
                    <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-300 mb-4">Dados e Backup</h3>
                    <div className="flex items-center justify-between p-4 bg-gray-100 dark:bg-gray-900/50 rounded-lg border border-gray-200 dark:border-gray-700">
                        <div>
                            <p className="text-gray-900 dark:text-white font-medium">Exportar Clientes</p>
                            <p className="text-gray-500 dark:text-gray-400 text-sm">Baixe a lista completa em formato CSV.</p>
                        </div>
                        <button
                            onClick={handleExportCSV}
                            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-lg font-medium transition-all shadow-lg shadow-blue-500/20"
                        >
                            <ArrowDownTrayIcon className="w-5 h-5" />
                            <span>Exportar (.csv)</span>
                        </button>
                    </div>
                </div>

                <div className="mt-6 pt-6 border-t border-gray-700">
                    <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-300 mb-4">Preferências</h3>
                    <div className="flex items-center justify-between p-4 bg-gray-100 dark:bg-gray-900/50 rounded-lg border border-gray-200 dark:border-gray-700">
                        <div>
                            <p className="text-gray-900 dark:text-white font-medium">Tema da Aplicação</p>
                            <p className="text-gray-500 dark:text-gray-400 text-sm">Escolha entre Claro e Escuro.</p>
                        </div>
                        <div className="flex bg-gray-200 dark:bg-gray-800 rounded-lg p-1">
                            <ThemeToggle />
                        </div>
                    </div>
                </div>

                <div className="mt-6">
                    <button
                        onClick={() => auth.signOut()}
                        className="w-full py-3 bg-red-600/20 hover:bg-red-600/30 text-red-500 border border-red-500/50 rounded-lg font-bold transition-all"
                    >
                        Sair da Conta
                    </button>
                </div>
            </div>

            <div className="text-center text-gray-500 text-sm">
                <p>IoriTV Admin Web v1.0.0</p>
                <p>&copy; 2024 ElevadorCom</p>
            </div>
        </div>
    );
}
