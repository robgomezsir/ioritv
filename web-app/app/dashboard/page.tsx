"use client";

import { useEffect, useState } from "react";
import { collection, onSnapshot, query, orderBy, getDoc, doc, setDoc, deleteDoc } from "firebase/firestore";
import { db } from "@/firebase/config";
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    Tooltip,
    ResponsiveContainer,
    Cell
} from "recharts";
import { onAuthStateChanged } from "firebase/auth";
import { auth } from "@/firebase/config";
import { useRouter } from "next/navigation";
import DespesaModal from "@/components/DespesaModal";

interface Cliente {
    SITUACAO: string;
    VALOR: number;
}

interface Despesa {
    id: string;
    data: string;
    descricao: string;
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

    useEffect(() => {
        const unsubscribeAuth = onAuthStateChanged(auth, (user) => {
            if (!user) router.push("/login");
        });

        const qClientes = query(collection(db, "clientes"));
        const unsubClientes = onSnapshot(qClientes, (snapshot) => {
            const data = snapshot.docs.map((doc) => doc.data() as Cliente);
            setClientes(data);
        });

        const qDespesas = query(collection(db, "despesas"), orderBy("dataTimestamp", "desc"));
        const unsubDespesas = onSnapshot(qDespesas, (snapshot) => {
            const data = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Despesa));
            setDespesas(data);
        });

        const loadCusto = async () => {
            const docRef = doc(db, "configuracoes", "custoTotal");
            const snap = await getDoc(docRef);
            if (snap.exists()) {
                setCustoTotalFixo(snap.data().valor || 0);
            }
        };
        const loadData = async () => {
            await loadCusto();
            setLoading(false);
        };
        loadData();

        return () => {
            unsubscribeAuth();
            unsubClientes();
            unsubDespesas();
        };
    }, [router]);

    // Client metrics
    const totalClientes = clientes.length;
    const situacaoCounts = {
        "ATIVO": 0,
        "A VENCER": 0,
        "VENCIDO": 0,
        "STANDBY": 0
    };

    clientes.forEach(c => {
        if (situacaoCounts.hasOwnProperty(c.SITUACAO)) {
            situacaoCounts[c.SITUACAO as keyof typeof situacaoCounts]++;
        }
    });

    // Financial metrics
    let totalVendas = 0;
    clientes.forEach(c => {
        if (c.SITUACAO === "ATIVO" || c.SITUACAO === "A VENCER") {
            totalVendas += c.VALOR || 0;
        }
    });

    const totalDespesas = despesas.reduce((acc, curr) => acc + curr.valor, 0);
    const lucroLiquido = totalVendas - custoTotalFixo;
    const lucroLiquidoFinal = lucroLiquido - totalDespesas;
    const margemLucro = totalVendas > 0 ? (lucroLiquidoFinal / totalVendas) * 100 : 0;

    const chartData = [
        { name: "Ativo", value: situacaoCounts["ATIVO"], color: "#10b981" },
        { name: "A Vencer", value: situacaoCounts["A VENCER"], color: "#f97316" },
        { name: "Vencido", value: situacaoCounts["VENCIDO"], color: "#ef4444" },
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

    const handleDeleteDespesa = (id: string) => {
        setDeleteConfirmation({ isOpen: true, id });
    };

    const executeDelete = async () => {
        if (deleteConfirmation.id) {
            try {
                await deleteDoc(doc(db, "despesas", deleteConfirmation.id));
                setDeleteConfirmation({ isOpen: false, id: null });
            } catch (error) {
                console.error("Erro ao excluir despesa:", error);
                alert("Erro ao excluir. Verifique o console.");
            }
        }
    };

    const formatCurrency = (val: number) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);

    let slogan = "";
    if (totalClientes <= 50) slogan = "Tem a sua cara!";
    else if (totalClientes <= 100) slogan = "É coisa nossa!";
    else slogan = "A gente se vê por aqui!";

    // Calculate percentages for progress bars
    const ativoPercent = totalClientes > 0 ? (situacaoCounts["ATIVO"] / totalClientes) * 100 : 0;
    const vencidoPercent = totalClientes > 0 ? (situacaoCounts["VENCIDO"] / totalClientes) * 100 : 0;
    const avencerPercent = totalClientes > 0 ? (situacaoCounts["A VENCER"] / totalClientes) * 100 : 0;

    // Payment status for pie chart
    const emDiaCount = situacaoCounts["ATIVO"];
    const pendentesCount = situacaoCounts["A VENCER"] + situacaoCounts["VENCIDO"];
    const paymentPercent = totalClientes > 0 ? (emDiaCount / totalClientes) * 100 : 0;

    if (loading) return <div className="p-8 text-center text-gray-900 dark:text-white">Carregando dados...</div>;

    return (
        <div className="flex flex-col h-screen overflow-hidden">
            {/* Fixed Header */}
            <header className="sticky top-0 z-10 flex flex-col md:flex-row justify-between items-center bg-white dark:bg-gray-800 p-6 border-b border-gray-200 dark:border-gray-700 shadow-md gap-4">
                <div>
                    <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-400 dark:to-purple-500 bg-clip-text text-transparent">
                        Visão Geral
                    </h1>
                    <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">Bem-vindo ao painel administrativo do IonTV.</p>
                </div>
                <div className="flex gap-3">
                    <button className="flex items-center gap-2 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-200 shadow-sm hover:bg-gray-50 dark:hover:bg-gray-700 transition-all">
                        📥 Exportar
                    </button>
                    <button
                        onClick={() => router.push('/clientes')}
                        className="flex items-center gap-2 rounded-xl bg-blue-600 hover:bg-blue-500 px-4 py-2 text-sm font-semibold text-white shadow-lg shadow-blue-500/20 transition-all"
                    >
                        ➕ Novo Cliente
                    </button>
                    <button onClick={() => auth.signOut()} className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg text-sm transition-all border border-gray-600">
                        Sair
                    </button>
                </div>
            </header>

            {/* Scrollable Content */}
            <div className="flex-1 overflow-y-auto p-6">
                <div className="max-w-[1600px] mx-auto space-y-8">
                    {/* Client Stats Cards */}
                    <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-5">
                        <div className="rounded-2xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-5 shadow-sm hover:shadow-lg transition-shadow">
                            <div className="mb-4 flex items-start justify-between">
                                <div>
                                    <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Total de Clientes</p>
                                    <h3 className="mt-2 text-3xl font-bold text-gray-900 dark:text-white">{totalClientes}</h3>
                                </div>
                                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-50 dark:bg-blue-900/20 text-blue-500">
                                    👥
                                </div>
                            </div>
                            <div className="flex items-center text-sm">
                                <span className="flex items-center font-medium text-purple-600 dark:text-purple-400">
                                    {slogan}
                                </span>
                            </div>
                        </div>

                        <div className="rounded-2xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-5 shadow-sm hover:shadow-lg transition-shadow">
                            <div className="mb-4 flex items-start justify-between">
                                <div>
                                    <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Ativos</p>
                                    <h3 className="mt-2 text-3xl font-bold text-gray-900 dark:text-white">{situacaoCounts["ATIVO"]}</h3>
                                </div>
                                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-green-50 dark:bg-green-900/20 text-green-500">
                                    ✓
                                </div>
                            </div>
                            <div className="mt-2 h-1.5 w-full rounded-full bg-gray-100 dark:bg-gray-700">
                                <div className="h-1.5 rounded-full bg-green-500" style={{ width: `${ativoPercent}%` }}></div>
                            </div>
                            <p className="mt-2 text-xs text-gray-400">{ativoPercent.toFixed(0)}% da base total</p>
                        </div>

                        <div className="rounded-2xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-5 shadow-sm hover:shadow-lg transition-shadow">
                            <div className="mb-4 flex items-start justify-between">
                                <div>
                                    <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Vencidos</p>
                                    <h3 className="mt-2 text-3xl font-bold text-gray-900 dark:text-white">{situacaoCounts["VENCIDO"]}</h3>
                                </div>
                                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-red-50 dark:bg-red-900/20 text-red-500">
                                    ⚠️
                                </div>
                            </div>
                            <div className="mt-2 h-1.5 w-full rounded-full bg-gray-100 dark:bg-gray-700">
                                <div className="h-1.5 rounded-full bg-red-500" style={{ width: `${vencidoPercent}%` }}></div>
                            </div>
                            <p className="mt-2 text-xs text-gray-400">Ação necessária</p>
                        </div>

                        <div className="rounded-2xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-5 shadow-sm hover:shadow-lg transition-shadow">
                            <div className="mb-4 flex items-start justify-between">
                                <div>
                                    <p className="text-sm font-medium text-gray-500 dark:text-gray-400">A Vencer</p>
                                    <h3 className="mt-2 text-3xl font-bold text-gray-900 dark:text-white">{situacaoCounts["A VENCER"]}</h3>
                                </div>
                                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-orange-50 dark:bg-orange-900/20 text-orange-500">
                                    ⏰
                                </div>
                            </div>
                            <div className="mt-2 h-1.5 w-full rounded-full bg-gray-100 dark:bg-gray-700">
                                <div className="h-1.5 rounded-full bg-orange-500" style={{ width: `${avencerPercent}%` }}></div>
                            </div>
                            <p className="mt-2 text-xs text-gray-400">Próximos 7 dias</p>
                        </div>

                        <div className="rounded-2xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-5 shadow-sm hover:shadow-lg transition-shadow">
                            <div className="mb-4 flex items-start justify-between">
                                <div>
                                    <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Inativos</p>
                                    <h3 className="mt-2 text-3xl font-bold text-gray-900 dark:text-white">{situacaoCounts["STANDBY"]}</h3>
                                </div>
                                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gray-50 dark:bg-gray-700 text-gray-500 dark:text-gray-400">
                                    ⏸️
                                </div>
                            </div>
                            <div className="mt-2 h-1.5 w-full rounded-full bg-gray-100 dark:bg-gray-700">
                                <div className="h-1.5 rounded-full bg-gray-500" style={{ width: `${totalClientes > 0 ? (situacaoCounts["STANDBY"] / totalClientes) * 100 : 0}%` }}></div>
                            </div>
                            <p className="mt-2 text-xs text-gray-400">Mais de 15 dias vencidos</p>
                        </div>
                    </div>

                    {/* Financial Metrics */}
                    <div className="grid grid-cols-1 gap-6 lg:grid-cols-4">
                        <div className="rounded-2xl bg-gradient-to-br from-blue-500 to-blue-600 p-6 text-white shadow-lg shadow-blue-500/20 hover:scale-[1.02] transition-transform">
                            <div className="flex items-center justify-between">
                                <p className="text-sm font-medium text-blue-100">Faturamento</p>
                                <span className="text-blue-200">📈</span>
                            </div>
                            <h3 className="mt-4 text-2xl font-bold">{formatCurrency(totalVendas)}</h3>
                            <p className="mt-1 text-xs text-blue-100">Clientes ativos e a vencer</p>
                        </div>

                        <div
                            onClick={() => { setNewCustoValue(custoTotalFixo.toString()); setIsEditCustoOpen(true); }}
                            className="rounded-2xl bg-gradient-to-br from-red-500 to-red-600 p-6 text-white shadow-lg shadow-red-500/20 hover:scale-[1.02] transition-transform cursor-pointer"
                        >
                            <div className="flex items-center justify-between">
                                <p className="text-sm font-medium text-red-100">Custo Operacional</p>
                                <span className="text-red-200">📉</span>
                            </div>
                            <h3 className="mt-4 text-2xl font-bold">{formatCurrency(custoTotalFixo)}</h3>
                            <p className="mt-1 text-xs text-red-100">Manutenção de servidores ✏️</p>
                        </div>

                        <div className="rounded-2xl bg-gradient-to-br from-orange-500 to-orange-600 p-6 text-white shadow-lg shadow-orange-500/20 hover:scale-[1.02] transition-transform">
                            <div className="flex items-center justify-between">
                                <p className="text-sm font-medium text-orange-100">Despesas Extras</p>
                                <span className="text-orange-200">🧾</span>
                            </div>
                            <h3 className="mt-4 text-2xl font-bold">{formatCurrency(totalDespesas)}</h3>
                            <p className="mt-1 text-xs text-orange-100">{despesas.length} despesa(s) registrada(s)</p>
                        </div>

                        <div className={`rounded-2xl bg-gradient-to-br ${lucroLiquidoFinal >= 0 ? 'from-green-500 to-green-600' : 'from-red-500 to-red-600'} p-6 text-white shadow-lg ${lucroLiquidoFinal >= 0 ? 'shadow-green-500/20' : 'shadow-red-500/20'} hover:scale-[1.02] transition-transform`}>
                            <div className="flex items-center justify-between">
                                <p className="text-sm font-medium text-green-100">Lucro Líquido</p>
                                <span className="text-green-200">💰</span>
                            </div>
                            <h3 className="mt-4 text-2xl font-bold">{formatCurrency(lucroLiquidoFinal)}</h3>
                            <div className="mt-2 inline-flex items-center rounded bg-white/20 px-2 py-0.5 text-xs font-semibold text-white">
                                Margem: {margemLucro.toFixed(2)}%
                            </div>
                        </div>
                    </div>

                    {/* Chart and Payment Status */}
                    <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
                        <div className="rounded-2xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm lg:col-span-2">
                            <div className="mb-6 flex items-center justify-between">
                                <h3 className="text-lg font-bold text-gray-900 dark:text-white">Distribuição de Status</h3>
                                <button className="text-xs font-medium text-purple-600 dark:text-purple-400 hover:text-purple-700 dark:hover:text-purple-300">Ver Relatórios</button>
                            </div>
                            <div className="h-80 w-full">
                                <ResponsiveContainer width="100%" height="100%">
                                    <BarChart data={chartData} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
                                        <XAxis
                                            dataKey="name"
                                            stroke="#888888"
                                            tick={{ fill: '#6b7280', fontSize: 14, fontWeight: 500 }}
                                            axisLine={{ stroke: '#d1d5db' }}
                                        />
                                        <YAxis stroke="#888888" tick={{ fill: '#6b7280' }} />
                                        <Tooltip
                                            contentStyle={{
                                                backgroundColor: '#111827',
                                                borderColor: '#374151',
                                                borderRadius: '0.5rem',
                                                color: '#f9fafb',
                                                boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.5)'
                                            }}
                                            itemStyle={{ color: '#e5e7eb' }}
                                            cursor={{ fill: 'rgba(255,255,255,0.1)' }}
                                        />
                                        <Bar dataKey="value" radius={[8, 8, 0, 0]}>
                                            {chartData.map((entry, index) => (
                                                <Cell key={`cell-${index}`} fill={entry.color} />
                                            ))}
                                        </Bar>
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        </div>

                        {/* Payment Status */}
                        <div className="rounded-2xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm">
                            <div className="mb-4 flex items-center justify-between">
                                <h3 className="text-lg font-bold text-gray-900 dark:text-white">Pagamentos</h3>
                                <span className="text-gray-400 cursor-pointer">⋯</span>
                            </div>
                            <div className="flex flex-col items-center justify-center py-6">
                                <div className="relative h-48 w-48">
                                    <svg className="h-full w-full -rotate-90" viewBox="0 0 36 36">
                                        <path
                                            className="text-gray-100 dark:text-gray-700"
                                            d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                                            fill="none"
                                            stroke="currentColor"
                                            strokeWidth="3.8"
                                        />
                                        <path
                                            className="text-green-500"
                                            d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                                            fill="none"
                                            stroke="currentColor"
                                            strokeDasharray={`${paymentPercent}, 100`}
                                            strokeLinecap="round"
                                            strokeWidth="3.8"
                                        />
                                        <path
                                            className="text-orange-500"
                                            d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                                            fill="none"
                                            stroke="currentColor"
                                            strokeDasharray={`${100 - paymentPercent}, 100`}
                                            strokeDashoffset={`-${paymentPercent}`}
                                            strokeLinecap="round"
                                            strokeWidth="3.8"
                                        />
                                    </svg>
                                    <div className="absolute inset-0 flex flex-col items-center justify-center">
                                        <span className="text-3xl font-bold text-gray-900 dark:text-white">{paymentPercent.toFixed(0)}%</span>
                                        <span className="text-xs text-gray-500">Pagos</span>
                                    </div>
                                </div>
                            </div>
                            <div className="mt-4 flex justify-center gap-4 text-xs">
                                <div className="flex items-center gap-1">
                                    <span className="h-2 w-2 rounded-full bg-green-500"></span>
                                    <span className="text-gray-600 dark:text-gray-400">Em dia ({emDiaCount})</span>
                                </div>
                                <div className="flex items-center gap-1">
                                    <span className="h-2 w-2 rounded-full bg-orange-500"></span>
                                    <span className="text-gray-600 dark:text-gray-400">Pendentes ({pendentesCount})</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Despesas Table */}
                    <div className="rounded-2xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 shadow-sm">
                        <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-700 p-6">
                            <h3 className="text-lg font-bold text-gray-900 dark:text-white">Despesas Operacionais</h3>
                            <button
                                onClick={() => { setDespesaToEdit(null); setIsDespesaModalOpen(true); }}
                                className="flex items-center gap-2 rounded-lg bg-purple-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-purple-700 transition-colors"
                            >
                                ➕ Nova Despesa
                            </button>
                        </div>
                        <div className="overflow-x-auto">
                            <table className="w-full text-left">
                                <thead>
                                    <tr className="bg-gray-50/50 dark:bg-gray-800/50 text-xs uppercase text-gray-500 dark:text-gray-400">
                                        <th className="px-6 py-4 font-semibold">Data</th>
                                        <th className="px-6 py-4 font-semibold">Descrição</th>
                                        <th className="px-6 py-4 font-semibold text-right">Valor</th>
                                        <th className="px-6 py-4 font-semibold text-center">Ações</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100 dark:divide-gray-700 text-sm">
                                    {despesas.length === 0 ? (
                                        <tr>
                                            <td className="px-6 py-12 text-center text-gray-500 dark:text-gray-400" colSpan={4}>
                                                <div className="flex flex-col items-center justify-center gap-2">
                                                    <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
                                                        <span className="text-2xl">🧾</span>
                                                    </div>
                                                    <p>Nenhuma despesa registrada.</p>
                                                </div>
                                            </td>
                                        </tr>
                                    ) : (
                                        despesas.slice(0, 5).map(d => (
                                            <tr key={d.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/50">
                                                <td className="px-6 py-4 text-gray-700 dark:text-gray-300">{d.data}</td>
                                                <td className="px-6 py-4 text-gray-700 dark:text-gray-300">{d.descricao}</td>
                                                <td className="px-6 py-4 text-right font-mono text-red-600 dark:text-red-400">- {formatCurrency(d.valor)}</td>
                                                <td className="px-6 py-4 text-center">
                                                    <button
                                                        onClick={() => handleDeleteDespesa(d.id)}
                                                        className="text-red-600 dark:text-red-400 hover:text-red-700 dark:hover:text-red-300 font-medium text-xs"
                                                    >
                                                        Excluir
                                                    </button>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                            {despesas.length > 5 && (
                                <div className="p-4 bg-gray-50 dark:bg-gray-800/50 text-center border-t border-gray-100 dark:border-gray-700">
                                    <button onClick={() => router.push('/financeiro')} className="text-purple-600 dark:text-purple-400 font-medium text-sm hover:underline">
                                        Ver todas as {despesas.length} despesas →
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            {/* Modals */}
            {deleteConfirmation.isOpen && (
                <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
                    <div className="bg-white dark:bg-gray-800 p-6 rounded-xl w-full max-w-sm border border-gray-200 dark:border-gray-700 shadow-2xl">
                        <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-2">Confirmar Exclusão</h3>
                        <p className="text-gray-600 dark:text-gray-300 mb-6">
                            Tem certeza que deseja excluir esta despesa? Esta ação não pode ser desfeita.
                        </p>
                        <div className="flex gap-3">
                            <button
                                onClick={() => setDeleteConfirmation({ isOpen: false, id: null })}
                                className="flex-1 py-2.5 bg-gray-200 dark:bg-gray-700 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors font-medium"
                            >
                                Cancelar
                            </button>
                            <button
                                onClick={executeDelete}
                                className="flex-1 py-2.5 bg-red-600 hover:bg-red-500 rounded-lg text-white shadow-lg hover:shadow-red-500/25 transition-all font-medium"
                            >
                                Sim, Excluir
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {isEditCustoOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
                    <div className="bg-white dark:bg-gray-800 p-6 rounded-xl w-full max-w-sm border border-gray-200 dark:border-gray-700 shadow-2xl">
                        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Definir Custo Operacional Mensal</h3>
                        <input
                            type="number"
                            value={newCustoValue}
                            onChange={e => setNewCustoValue(e.target.value)}
                            className="w-full bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg px-4 py-2 text-gray-900 dark:text-white mb-4 outline-none focus:ring-2 focus:ring-purple-500"
                            placeholder="0.00"
                        />
                        <div className="flex gap-2">
                            <button onClick={() => setIsEditCustoOpen(false)} className="flex-1 py-2 bg-gray-200 dark:bg-gray-700 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors">Cancelar</button>
                            <button onClick={handleSaveCusto} className="flex-1 py-2 bg-purple-600 rounded-lg text-white hover:bg-purple-700 transition-colors">Salvar</button>
                        </div>
                    </div>
                </div>
            )}

            <DespesaModal
                isOpen={isDespesaModalOpen}
                onClose={() => setIsDespesaModalOpen(false)}
                despesaToEdit={despesaToEdit}
                onSuccess={() => { }}
            />
        </div>
    );
}
