"use client";

import { useState, useEffect } from "react";
import { signInWithEmailAndPassword, sendPasswordResetEmail, createUserWithEmailAndPassword } from "firebase/auth";
import { auth } from "@/firebase/config";
import { useRouter } from "next/navigation";
import Image from "next/image";

export default function LoginPage() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [rememberMe, setRememberMe] = useState(false);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [showForgotPassword, setShowForgotPassword] = useState(false);
    const [showCreateAccount, setShowCreateAccount] = useState(false);
    const [resetEmail, setResetEmail] = useState("");
    const [newEmail, setNewEmail] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
    const router = useRouter();

    // Load saved email on mount
    useEffect(() => {
        const savedEmail = localStorage.getItem("rememberedEmail");
        if (savedEmail) {
            setEmail(savedEmail);
            setRememberMe(true);
        }
    }, []);

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError("");

        try {
            await signInWithEmailAndPassword(auth, email, password);

            // Save email if remember me is checked
            if (rememberMe) {
                localStorage.setItem("rememberedEmail", email);
            } else {
                localStorage.removeItem("rememberedEmail");
            }

            router.push("/dashboard");
        } catch (err) {
            setError("Falha ao fazer login. Verifique suas credenciais.");
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleForgotPassword = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError("");
        setSuccessMessage("");

        try {
            await sendPasswordResetEmail(auth, resetEmail);
            setSuccessMessage("Email de recuperação enviado! Verifique sua caixa de entrada.");
            setTimeout(() => {
                setShowForgotPassword(false);
                setResetEmail("");
                setSuccessMessage("");
            }, 3000);
        } catch (err) {
            setError("Erro ao enviar email de recuperação. Verifique o endereço.");
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleCreateAccount = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError("");
        setSuccessMessage("");

        if (newPassword !== confirmPassword) {
            setError("As senhas não coincidem.");
            setLoading(false);
            return;
        }

        if (newPassword.length < 6) {
            setError("A senha deve ter pelo menos 6 caracteres.");
            setLoading(false);
            return;
        }

        try {
            await createUserWithEmailAndPassword(auth, newEmail, newPassword);
            setSuccessMessage("Conta criada com sucesso! Redirecionando...");
            setTimeout(() => {
                router.push("/dashboard");
            }, 1500);
        } catch (err) {
            if (err && typeof err === 'object' && 'code' in err && err.code === "auth/email-already-in-use") {
                setError("Este email já está em uso.");
            } else {
                setError("Erro ao criar conta. Tente novamente.");
            }
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 px-4">
            {/* Logo */}
            <div className="absolute top-8 left-1/2 -translate-x-1/2">
                <Image
                    src="/logo.png"
                    alt="IoriTV Logo"
                    width={150}
                    height={50}
                    className="object-contain"
                    priority
                />
            </div>

            {/* Login Card */}
            <div className="max-w-md w-full bg-gray-100 dark:bg-gray-800 rounded-3xl shadow-2xl overflow-hidden p-8 sm:p-10">
                <div className="mb-8 text-center">
                    <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
                        Oi, Sinho! Bem-vindo de volta!
                    </h2>
                    <p className="text-gray-600 dark:text-gray-400 text-sm">
                        Faça login para continuar
                    </p>
                </div>

                <form onSubmit={handleLogin} className="space-y-5">
                    {/* Email Input */}
                    <div>
                        <div className="relative">
                            <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                                <span className="text-gray-400">📧</span>
                            </div>
                            <input
                                type="email"
                                required
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                className="w-full pl-12 pr-4 py-3.5 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-xl text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all"
                                placeholder="Email"
                            />
                        </div>
                    </div>

                    {/* Password Input */}
                    <div>
                        <div className="relative">
                            <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                                <span className="text-gray-400">🔒</span>
                            </div>
                            <input
                                type={showPassword ? "text" : "password"}
                                required
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                className="w-full pl-12 pr-12 py-3.5 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-xl text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all"
                                placeholder="Senha"
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="absolute inset-y-0 right-0 pr-4 flex items-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                            >
                                {showPassword ? "👁️" : "👁️‍🗨️"}
                            </button>
                        </div>
                    </div>

                    {/* Remember Me */}
                    <div className="flex items-center">
                        <input
                            type="checkbox"
                            id="remember"
                            checked={rememberMe}
                            onChange={(e) => setRememberMe(e.target.checked)}
                            className="h-4 w-4 text-purple-600 focus:ring-purple-500 border-gray-300 rounded"
                        />
                        <label htmlFor="remember" className="ml-2 block text-sm text-gray-700 dark:text-gray-300">
                            Manter-me conectado
                        </label>
                    </div>

                    {/* Error Message */}
                    {error && (
                        <div className="text-red-500 text-sm text-center bg-red-100 dark:bg-red-900/30 p-3 rounded-lg">
                            {error}
                        </div>
                    )}

                    {/* Login Button */}
                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-3.5 px-4 bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white font-semibold rounded-xl shadow-lg hover:shadow-xl transition-all transform hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                    >
                        <span>➜</span>
                        <span>{loading ? "Entrando..." : "Entrar"}</span>
                    </button>
                </form>

                {/* Footer Links */}
                <div className="mt-6 flex items-center justify-center gap-4 text-sm text-gray-600 dark:text-gray-400">
                    <button
                        onClick={() => setShowForgotPassword(true)}
                        className="hover:text-purple-600 dark:hover:text-purple-400 transition-colors"
                    >
                        Esqueci a senha
                    </button>
                    <span>•</span>
                    <button
                        onClick={() => setShowCreateAccount(true)}
                        className="hover:text-purple-600 dark:hover:text-purple-400 transition-colors"
                    >
                        Criar conta
                    </button>
                </div>
            </div>

            {/* Forgot Password Modal */}
            {showForgotPassword && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
                    <div className="bg-white dark:bg-gray-800 p-6 rounded-xl w-full max-w-md border border-gray-200 dark:border-gray-700 shadow-2xl">
                        <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-2">Recuperar Senha</h3>
                        <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
                            Digite seu email para receber um link de recuperação.
                        </p>
                        <form onSubmit={handleForgotPassword} className="space-y-4">
                            <input
                                type="email"
                                required
                                value={resetEmail}
                                onChange={(e) => setResetEmail(e.target.value)}
                                className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
                                placeholder="seu@email.com"
                            />
                            {successMessage && (
                                <div className="text-green-600 dark:text-green-400 text-sm bg-green-100 dark:bg-green-900/30 p-3 rounded-lg">
                                    {successMessage}
                                </div>
                            )}
                            {error && (
                                <div className="text-red-500 text-sm bg-red-100 dark:bg-red-900/30 p-3 rounded-lg">
                                    {error}
                                </div>
                            )}
                            <div className="flex gap-3">
                                <button
                                    type="button"
                                    onClick={() => { setShowForgotPassword(false); setResetEmail(""); setError(""); }}
                                    className="flex-1 py-2.5 bg-gray-200 dark:bg-gray-700 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors font-medium"
                                >
                                    Cancelar
                                </button>
                                <button
                                    type="submit"
                                    disabled={loading}
                                    className="flex-1 py-2.5 bg-purple-600 hover:bg-purple-700 rounded-lg text-white transition-colors font-medium disabled:opacity-50"
                                >
                                    {loading ? "Enviando..." : "Enviar"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Create Account Modal */}
            {showCreateAccount && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
                    <div className="bg-white dark:bg-gray-800 p-6 rounded-xl w-full max-w-md border border-gray-200 dark:border-gray-700 shadow-2xl">
                        <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-2">Criar Nova Conta</h3>
                        <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
                            Preencha os dados para criar sua conta.
                        </p>
                        <form onSubmit={handleCreateAccount} className="space-y-4">
                            <input
                                type="email"
                                required
                                value={newEmail}
                                onChange={(e) => setNewEmail(e.target.value)}
                                className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
                                placeholder="Email"
                            />
                            <input
                                type="password"
                                required
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
                                placeholder="Senha (mínimo 6 caracteres)"
                            />
                            <input
                                type="password"
                                required
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
                                placeholder="Confirmar senha"
                            />
                            {successMessage && (
                                <div className="text-green-600 dark:text-green-400 text-sm bg-green-100 dark:bg-green-900/30 p-3 rounded-lg">
                                    {successMessage}
                                </div>
                            )}
                            {error && (
                                <div className="text-red-500 text-sm bg-red-100 dark:bg-red-900/30 p-3 rounded-lg">
                                    {error}
                                </div>
                            )}
                            <div className="flex gap-3">
                                <button
                                    type="button"
                                    onClick={() => { setShowCreateAccount(false); setNewEmail(""); setNewPassword(""); setConfirmPassword(""); setError(""); }}
                                    className="flex-1 py-2.5 bg-gray-200 dark:bg-gray-700 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors font-medium"
                                >
                                    Cancelar
                                </button>
                                <button
                                    type="submit"
                                    disabled={loading}
                                    className="flex-1 py-2.5 bg-purple-600 hover:bg-purple-700 rounded-lg text-white transition-colors font-medium disabled:opacity-50"
                                >
                                    {loading ? "Criando..." : "Criar Conta"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Footer */}
            <div className="absolute bottom-4 left-1/2 -translate-x-1/2 text-center">
                <p className="text-xs text-gray-500">
                    ioritv.com® - Todos os direitos reservados - 2025/26 - v4.2
                </p>
            </div>
        </div>
    );
}
