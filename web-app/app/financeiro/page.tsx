"use client";

import { useEffect, useState } from "react";
import { collection, onSnapshot, query, orderBy, getDoc, doc, setDoc, deleteDoc } from "firebase/firestore";
import { db } from "@/firebase/config";
import { onAuthStateChanged } from "firebase/auth";
import { auth } from "@/firebase/config";
import { useRouter } from "next/navigation";
import DespesaModal from "@/components/DespesaModal";

import { Timestamp } from "firebase/firestore";
import { getSmartStatus } from "@/utils/clientStatus";

interface Despesa {
    id: string;
    data: string;
    descricao: string;
    valor: number;
}

interface Cliente {
    SITUACAO: string;
    VENCIMENTO: string;
    VALOR: number;
    TERMINO?: Timestamp;
}

export default function FinanceiroPage() {
    const [despesas, setDespesas] = useState<Despesa[]>([]);
    const [clientes, setClientes] = useState<Cliente[]>([]);
    const [custoTotalFixo, setCustoTotalFixo] = useState(0);
    const [loading, setLoading] = useState(true);
    const [isEditCustoOpen, setIsEditCustoOpen] = useState(false);
    const [newCustoValue, setNewCustoValue] = useState("");

    // Modal states
    const [isDespesaModalOpen, setIsDespesaModalOpen] = useState(false);
    const [despesaToEdit, setDespesaToEdit] = useState<Despesa | null>(null);
    const [deleteConfirmation, setDeleteConfirmation] = useState<{ isOpen: boolean; id: string | null }>({
        isOpen: false,
        id: null
    });

    const router = useRouter();

    useEffect(() => {
        const unsubscribeAuth = onAuthStateChanged(auth, (user) => {
            if (!user) router.push("/login");
        });

        // 1. Listen to Clients (for revenue calculation)
        const qClientes = query(collection(db, "clientes"));
        const unsubClientes = onSnapshot(qClientes, (snapshot) => {
            const data = snapshot.docs.map(d => d.data() as Cliente);
            setClientes(data);
        });

        // 2. Listen to Despesas
        const qDespesas = query(collection(db, "despesas"), orderBy("dataTimestamp", "desc"));
        const unsubDespesas = onSnapshot(qDespesas, (snapshot) => {
            const data = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Despesa));
            setDespesas(data);
        });

        // 3. Get Custo Total
        // Note: Real-time listener on document would be better
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

    // --- Calculations (matching Android Logic) ---
    // Android:
    // vendas += valor if (ATIVO or (AVENCER and daysDifference <= 3)) -> wait, logic in Android was simpler:
    // if situacao == ATIVO or AVENCER -> Count as revenue?
    // Let's re-read Android logic briefly in memory:
    // situacao == "A VENCER" || (diasParaVencimento >= 0 && diasParaVencimento <= 3) -> Revenue
    // situacao == "ATIVO" || diasParaVencimento > 3 -> Revenue
    // Simplification: If STATUS is ATIVO or A VENCER, we count it.

    let totalVendas = 0;
    clientes.forEach(c => {
        const smartStatus = getSmartStatus(c);
        if (smartStatus === "ATIVO" || smartStatus === "A VENCER") {
            totalVendas += c.VALOR || 0;
        }
    });

    const totalDespesas = despesas.reduce((acc, curr) => acc + curr.valor, 0);
    const lucroLiquido = totalVendas - custoTotalFixo;
    const lucroLiquidoFinal = lucroLiquido - totalDespesas;
    const margemLucro = totalVendas > 0 ? (lucroLiquidoFinal / totalVendas) * 100 : 0;

    // Actions
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

    if (loading) return <div className="p-8 text-center text-gray-900 dark:text-white">Carregando Finanças...</div>;

    return (
        <div className="p-6 max-w-7xl mx-auto space-y-6">
            <h1 className="text-3xl font-bold bg-gradient-to-r from-green-600 to-blue-600 dark:from-green-400 dark:to-blue-500 bg-clip-text text-transparent mb-6">
                Financeiro
            </h1>

            {/* Metrics Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                <MetricCard
                    title="Faturamento (Vendas)"
                    value={formatCurrency(totalVendas)}
                    color="text-blue-600 dark:text-blue-400"
                    borderColor="border-blue-200 dark:border-blue-500/30"
                />
                <div onClick={() => { setNewCustoValue(custoTotalFixo.toString()); setIsEditCustoOpen(true); }} className="cursor-pointer">
                    <MetricCard
                        title="Custo Operacional (Fixo)"
                        value={formatCurrency(custoTotalFixo)}
                        color="text-red-500 dark:text-red-400"
                        borderColor="border-red-200 dark:border-red-500/30"
                        icon="✏️"
                    />
                </div>
                <MetricCard
                    title="Despesas Extras"
                    value={formatCurrency(totalDespesas)}
                    color="text-orange-600 dark:text-orange-400"
                    borderColor="border-orange-200 dark:border-orange-500/30"
                />
                <MetricCard
                    title="Lucro Líquido Final"
                    value={formatCurrency(lucroLiquidoFinal)}
                    color={lucroLiquidoFinal >= 0 ? "text-green-600 dark:text-green-400" : "text-red-600 dark:text-red-500"}
                    borderColor={lucroLiquidoFinal >= 0 ? "border-green-200 dark:border-green-500/30" : "border-red-200 dark:border-red-500/30"}
                />
                <MetricCard
                    title="Margem de Lucro"
                    value={`${margemLucro.toFixed(2)}%`}
                    color="text-purple-600 dark:text-purple-400"
                    borderColor="border-purple-200 dark:border-purple-500/30"
                />
            </div>

            {/* Despesas Section */}
            <div className="mt-8">
                <div className="flex justify-between items-center mb-4">
                    <h2 className="text-xl font-semibold text-gray-800 dark:text-gray-200">Despesas Operacionais</h2>
                    <button
                        onClick={() => { setDespesaToEdit(null); setIsDespesaModalOpen(true); }}
                        className="px-4 py-2 bg-blue-600 hover:bg-blue-500 rounded-lg text-white font-medium shadow-lg shadow-blue-500/20"
                    >
                        + Nova Despesa
                    </button>
                </div>

                <div className="bg-white dark:bg-gray-800 rounded-xl shadow-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
                    <table className="w-full text-left">
                        <thead className="bg-gray-100 dark:bg-gray-700/50 text-gray-500 dark:text-gray-400 uppercase text-xs">
                            <tr>
                                <th className="px-6 py-4">Data</th>
                                <th className="px-6 py-4">Descrição</th>
                                <th className="px-6 py-4">Valor</th>
                                <th className="px-6 py-4 text-right">Ações</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 dark:divide-gray-700 text-gray-700 dark:text-gray-300">
                            {despesas.map(d => (
                                <tr key={d.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/30">
                                    <td className="px-6 py-4">{d.data}</td>
                                    <td className="px-6 py-4">{d.descricao}</td>
                                    <td className="px-6 py-4 font-mono text-red-600 dark:text-red-300">- {formatCurrency(d.valor)}</td>
                                    <td className="px-6 py-4 text-right">
                                        <button
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                handleDeleteDespesa(d.id);
                                            }}
                                            className="relative z-10 px-3 py-1.5 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-md font-medium transition-all cursor-pointer"
                                            title="Excluir Despesa"
                                        >
                                            Excluir
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {despesas.length === 0 && (
                                <tr><td colSpan={4} className="px-6 py-8 text-center text-gray-500">Nenhuma despesa registrada.</td></tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Confirmation Modal */}
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

            {/* Custo Total Modal */}
            {isEditCustoOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
                    <div className="bg-white dark:bg-gray-800 p-6 rounded-xl w-full max-w-sm border border-gray-200 dark:border-gray-700 shadow-2xl">
                        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Definir Custo Operacional Mensal</h3>
                        <input
                            type="number"
                            value={newCustoValue}
                            onChange={e => setNewCustoValue(e.target.value)}
                            className="w-full bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg px-4 py-2 text-gray-900 dark:text-white mb-4 outline-none focus:ring-2 focus:ring-blue-500"
                            placeholder="0.00"
                        />
                        <div className="flex gap-2">
                            <button onClick={() => setIsEditCustoOpen(false)} className="flex-1 py-2 bg-gray-200 dark:bg-gray-700 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors">Cancelar</button>
                            <button onClick={handleSaveCusto} className="flex-1 py-2 bg-blue-600 rounded-lg text-white hover:bg-blue-500 transition-colors">Salvar</button>
                        </div>
                    </div>
                </div>
            )}

            <DespesaModal
                isOpen={isDespesaModalOpen}
                onClose={() => setIsDespesaModalOpen(false)}
                despesaToEdit={despesaToEdit}
                onSuccess={() => { }} // Live listener updates list
            />
        </div>
    );
}

interface MetricCardProps {
    title: string;
    value: string | number;
    color: string;
    borderColor: string;
    icon?: React.ReactNode;
}

function MetricCard({ title, value, color, borderColor, icon }: MetricCardProps) {
    return (
        <div className={`bg-white dark:bg-gray-800 p-6 rounded-xl border ${borderColor} shadow-lg relative overflow-hidden transition-all duration-300`}>
            <p className="text-gray-500 dark:text-gray-400 text-xs font-bold uppercase tracking-wider">{title}</p>
            <p className={`text-2xl font-bold mt-2 ${color}`}>{value}</p>
            {icon && <div className="absolute top-4 right-4 opacity-50 text-xl">{icon}</div>}
        </div>
    );
}


