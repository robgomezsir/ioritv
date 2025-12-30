"use client";

import { useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import { doc, getDoc, updateDoc, Timestamp } from "firebase/firestore";
import { db } from "@/firebase/config";
import { onAuthStateChanged } from "firebase/auth";
import { auth } from "@/firebase/config";

interface ClienteData {
    NOME: string;
    USUARIO: string;
    SENHA: string;
    WHATSAPP: string;
    INICIO: Timestamp;
    CREDITOS: number;
    VALOR: number;
    CUSTO: number;
    DESCONTO: number;
    SERVIDOR: string;
    MAC: string;
    DEVICE: string;
    SITUACAO?: string;
    VENCIMENTO?: string;
    TERMINO?: Timestamp;
}

export default function ClienteDetailPage() {
    const router = useRouter();
    const params = useParams();
    const clientId = params.id as string;

    const [formData, setFormData] = useState<ClienteData>({
        NOME: "",
        USUARIO: "",
        SENHA: "",
        WHATSAPP: "",
        INICIO: Timestamp.now(),
        CREDITOS: 1,
        VALOR: 0,
        CUSTO: 0,
        DESCONTO: 0,
        SERVIDOR: "",
        MAC: "",
        DEVICE: ""
    });
    const [inicioDate, setInicioDate] = useState("");
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        const unsubscribeAuth = onAuthStateChanged(auth, (user) => {
            if (!user) router.push("/login");
        });

        const loadClient = async () => {
            try {
                const docRef = doc(db, "clientes", clientId);
                const docSnap = await getDoc(docRef);

                if (docSnap.exists()) {
                    const data = docSnap.data() as ClienteData;
                    setFormData(data);

                    if (data.INICIO) {
                        const date = data.INICIO.toDate();
                        setInicioDate(date.toISOString().split('T')[0]);
                    }
                } else {
                    alert("Cliente não encontrado");
                    router.push("/clientes");
                }
            } catch (error) {
                console.error("Erro ao carregar cliente:", error);
                alert("Erro ao carregar dados do cliente");
            } finally {
                setLoading(false);
            }
        };

        loadClient();
        return () => unsubscribeAuth();
    }, [clientId, router]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: name === "CREDITOS" || name === "VALOR" || name === "CUSTO" || name === "DESCONTO"
                ? parseFloat(value) || 0
                : value
        }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);

        try {
            const updateData: any = { ...formData };

            if (inicioDate) {
                const [year, month, day] = inicioDate.split('-').map(Number);
                updateData.INICIO = Timestamp.fromDate(new Date(year, month - 1, day));
            }

            const docRef = doc(db, "clientes", clientId);
            await updateDoc(docRef, updateData);

            alert("Cliente atualizado com sucesso!");
            router.push("/clientes");
        } catch (error) {
            console.error("Erro ao salvar:", error);
            alert("Erro ao salvar cliente. Verifique o console.");
        } finally {
            setSaving(false);
        }
    };

    const inputClass = "w-full bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg px-3 py-2 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 outline-none transition-all";
    const labelClass = "text-sm text-gray-500 dark:text-gray-400 block mb-1 font-medium";

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-gray-900 dark:text-white text-lg">Carregando...</div>
            </div>
        );
    }

    return (
        <div className="p-6 max-w-5xl mx-auto">
            <div className="mb-6 flex items-center justify-between">
                <div>
                    <button
                        onClick={() => router.push("/clientes")}
                        className="text-blue-600 dark:text-blue-400 hover:text-blue-500 dark:hover:text-blue-300 mb-2 flex items-center gap-2 transition-colors"
                    >
                        ← Voltar para Clientes
                    </button>
                    <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-400 dark:to-purple-500 bg-clip-text text-transparent">
                        Cadastro Completo do Cliente
                    </h1>
                </div>
                <div className="text-right">
                    <div className="text-sm text-gray-500 dark:text-gray-400">Status</div>
                    <div className={`inline-block px-3 py-1 rounded-full text-xs font-semibold mt-1 ${formData.SITUACAO === "ATIVO" ? "bg-green-500/10 text-green-600 dark:text-green-400 border border-green-500/20" :
                            formData.SITUACAO === "A VENCER" ? "bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 border border-yellow-500/20" :
                                formData.SITUACAO === "VENCIDO" ? "bg-red-500/10 text-red-600 dark:text-red-400 border border-red-500/20" :
                                    "bg-gray-500/10 text-gray-600 dark:text-gray-400 border border-gray-500/20"
                        }`}>
                        {formData.SITUACAO || "N/A"}
                    </div>
                    {formData.VENCIMENTO && (
                        <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                            Vencimento: {formData.VENCIMENTO}
                        </div>
                    )}
                </div>
            </div>

            <form onSubmit={handleSubmit} className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 shadow-xl p-8 space-y-6">
                <div className="border-b border-gray-200 dark:border-gray-700 pb-4">
                    <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Informações Básicas</h2>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                        <label className={labelClass}>Nome Completo</label>
                        <input name="NOME" value={formData.NOME || ""} onChange={handleChange} required className={inputClass} />
                    </div>
                    <div>
                        <label className={labelClass}>Usuário</label>
                        <input name="USUARIO" value={formData.USUARIO || ""} onChange={handleChange} required className={inputClass} />
                    </div>
                    <div>
                        <label className={labelClass}>Senha</label>
                        <input name="SENHA" value={formData.SENHA || ""} onChange={handleChange} className={inputClass} />
                    </div>
                    <div>
                        <label className={labelClass}>WhatsApp</label>
                        <input name="WHATSAPP" value={formData.WHATSAPP || ""} onChange={handleChange} className={inputClass} placeholder="(00) 00000-0000" />
                    </div>
                </div>

                <div className="border-b border-gray-200 dark:border-gray-700 pb-4 pt-4">
                    <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Assinatura e Pagamento</h2>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div>
                        <label className={labelClass}>Data de Início</label>
                        <input type="date" value={inicioDate} onChange={(e) => setInicioDate(e.target.value)} required className={inputClass} />
                    </div>
                    <div>
                        <label className={labelClass}>Créditos (Meses)</label>
                        <input type="number" name="CREDITOS" value={formData.CREDITOS} onChange={handleChange} required className={inputClass} min="1" />
                    </div>
                    <div>
                        <label className={labelClass}>Valor (R$)</label>
                        <input type="number" step="0.01" name="VALOR" value={formData.VALOR} onChange={handleChange} className={inputClass} />
                    </div>
                    <div>
                        <label className={labelClass}>Custo (R$)</label>
                        <input type="number" step="0.01" name="CUSTO" value={formData.CUSTO || 0} onChange={handleChange} className={inputClass} />
                    </div>
                    <div>
                        <label className={labelClass}>Desconto (R$)</label>
                        <input type="number" step="0.01" name="DESCONTO" value={formData.DESCONTO || 0} onChange={handleChange} className={inputClass} />
                    </div>
                </div>

                <div className="border-b border-gray-200 dark:border-gray-700 pb-4 pt-4">
                    <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Informações Técnicas</h2>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                        <label className={labelClass}>Servidor</label>
                        <input name="SERVIDOR" value={formData.SERVIDOR || ""} onChange={handleChange} className={inputClass} placeholder="URL do servidor" />
                    </div>
                    <div>
                        <label className={labelClass}>MAC Address</label>
                        <input name="MAC" value={formData.MAC || ""} onChange={handleChange} className={inputClass} placeholder="00:00:00:00:00:00" />
                    </div>
                    <div className="md:col-span-2">
                        <label className={labelClass}>Device ID</label>
                        <input name="DEVICE" value={formData.DEVICE || ""} onChange={handleChange} className={inputClass} />
                    </div>
                </div>

                <div className="pt-6 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3">
                    <button
                        type="button"
                        onClick={() => router.push("/clientes")}
                        className="px-6 py-2.5 bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors font-medium"
                    >
                        Cancelar
                    </button>
                    <button
                        type="submit"
                        disabled={saving}
                        className="px-6 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-lg font-medium shadow-lg hover:shadow-blue-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {saving ? "Salvando..." : "Salvar Alterações"}
                    </button>
                </div>
            </form>
        </div>
    );
}
