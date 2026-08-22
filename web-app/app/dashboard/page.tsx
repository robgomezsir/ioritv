"use client";

import { useEffect, useState } from "react";
import { collection, onSnapshot, query, orderBy, getDoc, doc, setDoc, deleteDoc } from "firebase/firestore";
import { db } from "@/firebase/config";
import {
    BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell
} from "recharts";
import { onAuthStateChanged } from "firebase/auth";
import { auth } from "@/firebase/config";
import { useRouter } from "next/navigation";
import DespesaModal from "@/components/DespesaModal";
import { getSmartStatus } from "@/utils/clientStatus";
import { Timestamp } from "firebase/firestore";
import { useGlass } from "@/components/ThemeProvider";

interface Cliente {
    SITUACAO: string;
    VALOR: number;
    TERMINO?: Timestamp;
}

interface Despesa {
    id: string;
    data: string;
    descricao: string;
    detalhes?: string;
    valor: number;
}

export default function DashboardHome() {
    const [clientes, setClientes] = useState<Cliente[]>([]);
    const [despesas, setDespesas] = useState<Despesa[]>([]);
    const [custoTotalFixo, setCustoTotalFixo] = useState(0);
    const [loading, setLoading] = useState(true);
    const [isDespesaModalOpen, setIsDespesaModalOpen] = useState(false);
    const [despesaToEdit, setDespesaToEdit] = useState<Despesa | null>(null);
    const [deleteConfirmation, setDeleteConfirmation] = useState<{ isOpen: boolean; id: string | null }>({
        isOpen: false,
        id: null
    });
    const [isEditCustoOpen, setIsEditCustoOpen] = useState(false);
    const [newCustoValue, setNewCustoValue] = useState("");
    const router = useRouter();
    useGlass();

    useEffect(() => {
        const unsubscribeAuth = onAuthStateChanged(auth, (user) => {
            if (!user) router.push("/login");
        });

        const qClientes = query(collection(db, "clientes"));
        const unsubClientes = onSnapshot(qClientes, (snapshot) => {
            const data = snapshot.docs.map((d) => d.data() as Cliente);
            setClientes(data);
            setLoading(false);
        });

        const qDespesas = query(collection(db, "despesas"), orderBy("dataTimestamp", "desc"));
        const unsubDespesas = onSnapshot(qDespesas, (snapshot) => {
            const data = snapshot.docs.map(d => ({ id: d.id, ...d.data() } as Despesa));
            setDespesas(data);
        });

        const loadCusto = async () => {
            const docRef = doc(db, "configuracoes", "custoTotal");
            const snap = await getDoc(docRef);
            if (snap.exists()) setCustoTotalFixo(snap.data().valor || 0);
        };
        loadCusto();

        return () => { unsubscribeAuth(); unsubClientes(); unsubDespesas(); };
    }, [router]);

    // ── Métricas de clientes (idênticas ao HomeFragment.kt) ──
    const totalClientes = clientes.length;
    const situacaoCounts = { "ATIVO": 0, "A VENCER": 0, "VENCIDO": 0, "STANDBY": 0 };
    let totalVendas = 0;
    let adimplentes = 0;

    clientes.forEach(c => {
        const smartStatus = getSmartStatus(c);
        if (situacaoCounts.hasOwnProperty(smartStatus)) {
            situacaoCounts[smartStatus as keyof typeof situacaoCounts]++;
        }
        if (smartStatus === "ATIVO" || smartStatus === "A VENCER") {
            totalVendas += c.VALOR || 0;
            adimplentes++;
        }
    });

    // ── Métricas financeiras (idênticas ao HomeFragment.kt) ──
    const totalDespesas = despesas.reduce((acc, curr) => acc + curr.valor, 0);
    const adimplencia = totalClientes > 0 ? (adimplentes * 100 / totalClientes) : 0;
    const margem = totalVendas > 0 ? ((totalVendas - custoTotalFixo - totalDespesas) * 100 / totalVendas) : 0;
    const lucroLiquido = totalVendas - custoTotalFixo;
    const lucroLiquidoFinal = lucroLiquido - totalDespesas;

    // ── Status de saúde financeira (idêntico ao HomeFragment.kt) ──
    const adimplenciaInt = Math.round(adimplencia);
    let statusLabel = "";
    let statusColor = "";
    if (adimplenciaInt >= 80) { statusLabel = "Excelente"; statusColor = "text-emerald-400"; }
    else if (adimplenciaInt >= 50) { statusLabel = "Em observação"; statusColor = "text-amber-400"; }
    else { statusLabel = "Precisa de atenção"; statusColor = "text-red-400"; }

    const chartData = [
        { name: "Ativo", value: situacaoCounts["ATIVO"], color: "#2E9E5A" },
        { name: "A Vencer", value: situacaoCounts["A VENCER"], color: "#E8913A" },
        { name: "Vencido", value: situacaoCounts["VENCIDO"], color: "#BA1A1A" },
        { name: "Standby", value: situacaoCounts["STANDBY"], color: "#6b7280" }
    ];

    const handleSaveCusto = async () => {
        const valor = parseFloat(newCustoValue);
        if (!isNaN(valor)) {
            await setDoc(doc(db, "configuracoes", "custoTotal"), { valor, ultimaAtualizacao: Date.now() });
            setCustoTotalFixo(valor);
            setIsEditCustoOpen(false);
        }
    };

    const executeDelete = async () => {
        if (deleteConfirmation.id) {
            try {
                await deleteDoc(doc(db, "despesas", deleteConfirmation.id));
                setDeleteConfirmation({ isOpen: false, id: null });
            } catch (error) {
                console.error("Erro ao excluir despesa:", error);
            }
        }
    };

    const fmt = (val: number) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);

    if (loading) return (
        <div className="flex items-center justify-center h-screen">
            <div className="glass-card p-8 text-center">
                <div className="animate-spin h-8 w-8 border-2 border-[var(--primary)] border-t-transparent rounded-full mx-auto mb-4"></div>
                <p className="text-[var(--on-surface-variant)]">Carregando dados...</p>
            </div>
        </div>
    );

    return (
        <div className="flex flex-col h-screen overflow-hidden">
            {/* ── Header ── */}
            <header className="glass-header sticky top-0 z-10 flex flex-col md:flex-row justify-between items-center px-6 py-4 gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-[var(--on-surface)]">
                        Visão Geral
                    </h1>
                    <p className="text-sm text-[var(--on-surface-variant)]">Painel administrativo IORI.Tv</p>
                </div>
                <div className="flex gap-3">
                    <button onClick={() => router.push('/clientes')} className="glass-btn-gold text-sm">
                        ➕ Novo Cliente
                    </button>
                    <button onClick={() => auth.signOut()} className="glass-input text-sm px-4 py-2 cursor-pointer hover:bg-[var(--surface-container-high)]">
                        Sair
                    </button>
                </div>
            </header>

            {/* ── Conteúdo ── */}
            <div className="flex-1 overflow-y-auto p-6">
                <div className="max-w-[1600px] mx-auto space-y-6">

                    {/* ── Saúde Financeira (card principal) ── */}
                    <div className="glass-card p-6">
                        <div className="flex flex-col lg:flex-row items-start lg:items-center gap-6">
                            {/* Anel de progresso */}
                            <div className="relative h-32 w-32 shrink-0">
                                <svg className="h-full w-full -rotate-90" viewBox="0 0 36 36">
                                    <path className="text-[var(--surface-container)]" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="currentColor" strokeWidth="3.8" />
                                    <path
                                        className={adimplenciaInt >= 80 ? "text-emerald-500" : adimplenciaInt >= 50 ? "text-amber-500" : "text-red-500"}
                                        d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                                        fill="none" stroke="currentColor" strokeDasharray={`${adimplenciaInt}, 100`} strokeLinecap="round" strokeWidth="3.8"
                                    />
                                </svg>
                                <div className="absolute inset-0 flex flex-col items-center justify-center">
                                    <span className="text-2xl font-bold text-[var(--on-surface)]">{adimplenciaInt}%</span>
                                    <span className={`text-xs font-semibold ${statusColor}`}>{statusLabel}</span>
                                </div>
                            </div>
                            {/* Info */}
                            <div className="flex-1 space-y-2">
                                <h2 className="text-lg font-bold text-[var(--on-surface)]">Saúde Financeira</h2>
                                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                                    <div>
                                        <p className="text-[var(--on-surface-variant)]">Faturamento</p>
                                        <p className="font-bold text-[var(--on-surface)]">{fmt(totalVendas)}</p>
                                    </div>
                                    <div
                                        className="cursor-pointer hover:bg-white/5 rounded-lg px-2 py-1 -mx-2 -my-1 transition-colors group"
                                        onClick={() => { setNewCustoValue(custoTotalFixo.toFixed(2)); setIsEditCustoOpen(true); }}
                                        title="Clique para editar o custo fixo"
                                    >
                                        <p className="text-[var(--on-surface-variant)]">Custo Fixo ✏️</p>
                                        <p className="font-bold text-[var(--on-surface)] group-hover:text-[var(--primary)] transition-colors">{fmt(custoTotalFixo)}</p>
                                    </div>
                                    <div>
                                        <p className="text-[var(--on-surface-variant)]">Despesas</p>
                                        <p className="font-bold text-[var(--on-surface)]">{fmt(totalDespesas)}</p>
                                    </div>
                                    <div>
                                        <p className="text-[var(--on-surface-variant)]">Lucro Líquido</p>
                                        <p className={`font-bold ${lucroLiquidoFinal >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>{fmt(lucroLiquidoFinal)}</p>
                                    </div>
                                </div>
                                <p className="text-xs text-[var(--on-surface-variant)]">Margem: {margem.toFixed(1)}%</p>
                            </div>
                        </div>
                    </div>

                    {/* ── Cards de Status ── */}
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                        {[
                            { label: "Ativos", count: situacaoCounts["ATIVO"], icon: "✓", color: "text-emerald-400", pct: totalClientes > 0 ? (situacaoCounts["ATIVO"] / totalClientes) * 100 : 0 },
                            { label: "A Vencer", count: situacaoCounts["A VENCER"], icon: "⏰", color: "text-amber-400", pct: totalClientes > 0 ? (situacaoCounts["A VENCER"] / totalClientes) * 100 : 0 },
                            { label: "Vencidos", count: situacaoCounts["VENCIDO"], icon: "⚠", color: "text-red-400", pct: totalClientes > 0 ? (situacaoCounts["VENCIDO"] / totalClientes) * 100 : 0 },
                            { label: "Standby", count: situacaoCounts["STANDBY"], icon: "⏸", color: "text-gray-400", pct: totalClientes > 0 ? (situacaoCounts["STANDBY"] / totalClientes) * 100 : 0 },
                        ].map((item) => (
                            <div key={item.label} className="glass-card p-4">
                                <div className="flex items-center justify-between mb-3">
                                    <p className="text-xs text-[var(--on-surface-variant)] font-medium">{item.label}</p>
                                    <span className={`text-lg ${item.color}`}>{item.icon}</span>
                                </div>
                                <p className="text-2xl font-bold text-[var(--on-surface)]">{item.count}</p>
                                <div className="mt-2 h-1.5 w-full rounded-full bg-[var(--surface-container)]">
                                    <div className={`h-1.5 rounded-full ${item.color.replace('text-', 'bg-')}`} style={{ width: `${item.pct}%` }}></div>
                                </div>
                                <p className="mt-1 text-[10px] text-[var(--on-surface-variant)]">{item.pct.toFixed(0)}% de {totalClientes}</p>
                            </div>
                        ))}
                    </div>

                    {/* ── Gráfico + Status de Pagamento ── */}
                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
                        {/* Gráfico de Barras */}
                        <div className="glass-card p-6 lg:col-span-2">
                            <h3 className="text-sm font-semibold text-[var(--on-surface)] mb-4">Distribuição de Clientes</h3>
                            <div className="h-64">
                                <ResponsiveContainer width="100%" height="100%">
                                    <BarChart data={chartData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                                        <XAxis dataKey="name" tick={{ fill: 'var(--on-surface-variant)', fontSize: 12 }} axisLine={false} tickLine={false} />
                                        <YAxis tick={{ fill: 'var(--on-surface-variant)', fontSize: 12 }} axisLine={false} tickLine={false} />
                                        <Tooltip
                                            contentStyle={{ background: 'var(--surface-container-high)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px', color: 'var(--on-surface)' }}
                                            cursor={{ fill: 'rgba(255,255,255,0.05)' }}
                                        />
                                        <Bar dataKey="value" radius={[6, 6, 0, 0]} label={{ position: 'top', fill: 'var(--on-surface)', fontSize: 14, fontWeight: 700 }}>
                                            {chartData.map((entry, i) => <Cell key={i} fill={entry.color} />)}
                                        </Bar>
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        </div>

                        {/* Pagamentos */}
                        <div className="glass-card p-6 flex flex-col items-center justify-center">
                            <h3 className="text-sm font-semibold text-[var(--on-surface)] mb-4">Pagamentos</h3>
                            <div className="relative h-40 w-40">
                                <svg className="h-full w-full -rotate-90" viewBox="0 0 36 36">
                                    <path className="text-[var(--surface-container)]" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="currentColor" strokeWidth="3.8" />
                                    <path className="text-emerald-500" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="currentColor" strokeDasharray={`${totalClientes > 0 ? (situacaoCounts["ATIVO"] / totalClientes) * 100 : 0}, 100`} strokeLinecap="round" strokeWidth="3.8" />
                                    <path className="text-amber-500" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="currentColor" strokeDasharray={`${(situacaoCounts["A VENCER"] + situacaoCounts["VENCIDO"]) / totalClientes * 100 || 0}, 100`} strokeDashoffset={`-${situacaoCounts["ATIVO"] / totalClientes * 100 || 0}`} strokeLinecap="round" strokeWidth="3.8" />
                                </svg>
                                <div className="absolute inset-0 flex flex-col items-center justify-center">
                                    <span className="text-xl font-bold text-[var(--on-surface)]">{totalClientes > 0 ? ((situacaoCounts["ATIVO"] / totalClientes) * 100).toFixed(0) : 0}%</span>
                                    <span className="text-[10px] text-[var(--on-surface-variant)]">Em dia</span>
                                </div>
                            </div>
                            <div className="flex gap-4 mt-4 text-xs">
                                <div className="flex items-center gap-1.5">
                                    <span className="h-2 w-2 rounded-full bg-emerald-500"></span>
                                    <span className="text-[var(--on-surface-variant)]">Em dia ({situacaoCounts["ATIVO"]})</span>
                                </div>
                                <div className="flex items-center gap-1.5">
                                    <span className="h-2 w-2 rounded-full bg-amber-500"></span>
                                    <span className="text-[var(--on-surface-variant)]">Pendentes ({situacaoCounts["A VENCER"] + situacaoCounts["VENCIDO"]})</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* ── Despesas ── */}
                    <div className="glass-card">
                        <div className="flex items-center justify-between p-5 border-b border-white/5">
                            <h3 className="text-sm font-semibold text-[var(--on-surface)]">Despesas Operacionais ({despesas.length})</h3>
                            <button onClick={() => { setDespesaToEdit(null); setIsDespesaModalOpen(true); }} className="glass-btn-primary text-xs px-3 py-1.5">
                                ➕ Nova Despesa
                            </button>
                        </div>
                        <div className="overflow-x-auto">
                            <table className="glass-table w-full text-left text-sm">
                                <thead>
                                    <tr className="text-[10px] uppercase tracking-wider text-[var(--on-surface-variant)]">
                                        <th className="px-5 py-3 font-semibold">Data</th>
                                        <th className="px-5 py-3 font-semibold">Descrição</th>
                                        <th className="px-5 py-3 font-semibold text-right">Valor</th>
                                        <th className="px-5 py-3 font-semibold text-center">Ações</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-white/5 text-[var(--on-surface)]">
                                    {despesas.length === 0 ? (
                                        <tr>
                                            <td className="px-5 py-10 text-center text-[var(--on-surface-variant)]" colSpan={4}>
                                                Nenhuma despesa registrada.
                                            </td>
                                        </tr>
                                    ) : (
                                        despesas.slice(0, 8).map(d => (
                                            <tr key={d.id} className="hover:bg-white/5">
                                                <td className="px-5 py-3 text-[var(--on-surface-variant)]">{d.data}</td>
                                                <td className="px-5 py-3">
                                                    <p>{d.descricao}</p>
                                                    {d.detalhes && <p className="text-xs text-[var(--on-surface-variant)] mt-0.5">{d.detalhes}</p>}
                                                </td>
                                                <td className="px-5 py-3 text-right font-mono text-red-400">- {fmt(d.valor)}</td>
                                                <td className="px-5 py-3 text-center">
                                                    <button onClick={() => setDeleteConfirmation({ isOpen: true, id: d.id })} className="text-red-400 hover:text-red-300 text-xs">Excluir</button>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            {/* ── Modal: Excluir ── */}
            {deleteConfirmation.isOpen && (
                <div className="glass-modal-overlay fixed inset-0 z-[60] flex items-center justify-center p-4">
                    <div className="glass-modal p-6 w-full max-w-sm">
                        <h3 className="text-lg font-bold text-[var(--on-surface)] mb-2">Confirmar Exclusão</h3>
                        <p className="text-sm text-[var(--on-surface-variant)] mb-6">Tem certeza que deseja excluir esta despesa?</p>
                        <div className="flex gap-3">
                            <button onClick={() => setDeleteConfirmation({ isOpen: false, id: null })} className="flex-1 glass-input text-center text-sm cursor-pointer">Cancelar</button>
                            <button onClick={executeDelete} className="flex-1 py-2.5 bg-red-600 hover:bg-red-500 rounded-xl text-white text-sm font-medium transition-all">Excluir</button>
                        </div>
                    </div>
                </div>
            )}

            {/* ── Modal: Custo ── */}
            {isEditCustoOpen && (
                <div className="glass-modal-overlay fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="glass-modal p-6 w-full max-w-sm">
                        <h3 className="text-lg font-semibold text-[var(--on-surface)] mb-4">Custo Operacional Mensal</h3>
                        <input type="number" value={newCustoValue} onChange={e => setNewCustoValue(e.target.value)} className="glass-input w-full mb-4" placeholder="0.00" />
                        <div className="flex gap-2">
                            <button onClick={() => setIsEditCustoOpen(false)} className="flex-1 glass-input text-center text-sm cursor-pointer">Cancelar</button>
                            <button onClick={handleSaveCusto} className="flex-1 glass-btn-primary text-sm">Salvar</button>
                        </div>
                    </div>
                </div>
            )}

            <DespesaModal isOpen={isDespesaModalOpen} onClose={() => setIsDespesaModalOpen(false)} despesaToEdit={despesaToEdit} onSuccess={() => { }} />
        </div>
    );
}
