"use client";

import { useEffect, useState } from "react";
import { collection, onSnapshot, query, orderBy, Timestamp, doc, deleteDoc } from "firebase/firestore";
import { db } from "@/firebase/config";
import { useRouter } from "next/navigation";
import { onAuthStateChanged } from "firebase/auth";
import { auth } from "@/firebase/config";
import ClientFormModal from "@/components/ClientFormModal";

// Type definition matches Android model somewhat
interface Cliente {
    id: string;
    NOME: string;
    USUARIO: string;
    SITUACAO: string;
    VENCIMENTO: string; // Cloud Function calculated string
    TERMINO?: Timestamp;
    CREDITOS: number;
    [key: string]: any;
}

export default function ClientesPage() {
    const [clientes, setClientes] = useState<Cliente[]>([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [clientToEdit, setClientToEdit] = useState<Cliente | null>(null);
    const [searchTerm, setSearchTerm] = useState("");
    const [filterStatus, setFilterStatus] = useState("TODOS");
    const [sortConfig, setSortConfig] = useState<{ key: string; direction: 'asc' | 'desc' } | null>(null);
    const [deleteConfirmation, setDeleteConfirmation] = useState<{ isOpen: boolean; id: string | null }>({
        isOpen: false,
        id: null
    });
    const router = useRouter();

    useEffect(() => {
        const unsubscribeAuth = onAuthStateChanged(auth, (user) => {
            if (!user) {
                router.push("/login"); // Redirect if not logged in
            }
        });

        // Real-time listener
        const q = query(collection(db, "clientes"), orderBy("NOME"));
        const unsubscribeSnapshot = onSnapshot(q, (snapshot) => {
            const data = snapshot.docs.map((doc) => ({
                id: doc.id,
                ...doc.data(),
            })) as Cliente[];
            setClientes(data);
            setLoading(false);
        });

        return () => {
            unsubscribeAuth();
            unsubscribeSnapshot();
        };
    }, [router]);

    const handleDelete = (id: string) => {
        setDeleteConfirmation({ isOpen: true, id });
    };

    const executeDelete = async () => {
        if (deleteConfirmation.id) {
            try {
                await deleteDoc(doc(db, "clientes", deleteConfirmation.id));
                setDeleteConfirmation({ isOpen: false, id: null });
            } catch (error) {
                console.error("Erro ao excluir cliente:", error);
                alert("Erro ao excluir. Verifique o console.");
            }
        }
    };

    const openNewClient = () => {
        setClientToEdit(null);
        setIsModalOpen(true);
    };

    const openEditClient = (cliente: Cliente) => {
        setClientToEdit(cliente);
        setIsModalOpen(true);
    };

    // Helper for status badge color
    const getStatusColor = (situacao: string) => {
        switch (situacao) {
            case "ATIVO": return "bg-green-500/10 dark:bg-green-500/20 text-green-600 dark:text-green-400 border-green-500/20 dark:border-green-500/50";
            case "A VENCER": return "bg-yellow-500/10 dark:bg-yellow-500/20 text-yellow-600 dark:text-yellow-400 border-yellow-500/20 dark:border-yellow-500/50";
            case "VENCIDO": return "bg-red-500/10 dark:bg-red-500/20 text-red-600 dark:text-red-400 border-red-500/20 dark:border-red-500/50";
            case "STANDBY": return "bg-gray-500/10 dark:bg-gray-500/20 text-gray-600 dark:text-gray-400 border-gray-500/20 dark:border-gray-500/50";
            default: return "bg-gray-500/10 dark:bg-gray-500/20 text-gray-600 dark:text-gray-400";
        }
    };

    const requestSort = (key: string) => {
        let direction: 'asc' | 'desc' = 'asc';
        if (sortConfig && sortConfig.key === key && sortConfig.direction === 'asc') {
            direction = 'desc';
        }
        setSortConfig({ key, direction });
    };

    const getSortIcon = (key: string) => {
        if (!sortConfig || sortConfig.key !== key) return <span className="ml-1 text-gray-600">↕</span>;
        return sortConfig.direction === 'asc' ? <span className="ml-1 text-blue-400">↑</span> : <span className="ml-1 text-blue-400">↓</span>;
    };

    if (loading) return <div className="min-h-screen bg-gray-900 flex items-center justify-center text-white">Carregando...</div>;

    const filteredClientes = clientes.filter(cliente => {
        const matchesSearch =
            cliente.NOME.toLowerCase().includes(searchTerm.toLowerCase()) ||
            cliente.USUARIO?.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesStatus = filterStatus === "TODOS" || cliente.SITUACAO === filterStatus;
        return matchesSearch && matchesStatus;
    }).sort((a, b) => {
        if (!sortConfig) return 0;

        let aValue = a[sortConfig.key];
        let bValue = b[sortConfig.key];

        // Specific handling for dates if available
        if (sortConfig.key === 'VENCIMENTO') {
            // Sort by TERMINO timestamp if available, otherwise fallback
            aValue = a.TERMINO ? a.TERMINO.seconds : 0;
            bValue = b.TERMINO ? b.TERMINO.seconds : 0;
        }

        if (aValue < bValue) {
            return sortConfig.direction === 'asc' ? -1 : 1;
        }
        if (aValue > bValue) {
            return sortConfig.direction === 'asc' ? 1 : -1;
        }
        return 0;
    });

    return (
        <div className="flex flex-col h-screen overflow-hidden">
            {/* Fixed Header */}
            <header className="sticky top-0 z-10 flex flex-col md:flex-row justify-between items-center bg-white dark:bg-gray-800 p-6 border-b border-gray-200 dark:border-gray-700 shadow-md gap-4">
                <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-400 dark:to-purple-500 bg-clip-text text-transparent">
                    Gestão de Clientes
                </h1>
                <div className="flex flex-col md:flex-row gap-4 w-full md:w-auto">
                    <div className="relative">
                        <input
                            type="text"
                            placeholder="Buscar por nome ou usuário..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 text-gray-900 dark:text-white text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full pl-10 p-2.5"
                        />
                        <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
                            <svg className="w-4 h-4 text-gray-500" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                <path stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="m19 19-4-4m0-7A7 7 0 1 1 1 8a7 7 0 0 1 14 0Z" />
                            </svg>
                        </div>
                    </div>

                    <select
                        value={filterStatus}
                        onChange={(e) => setFilterStatus(e.target.value)}
                        className="bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 text-gray-900 dark:text-white text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block p-2.5"
                    >
                        <option value="TODOS">Todos os Status</option>
                        <option value="ATIVO">Ativo</option>
                        <option value="A VENCER">A Vencer</option>
                        <option value="VENCIDO">Vencido</option>
                        <option value="STANDBY">Standby</option>
                    </select>

                    <button
                        onClick={openNewClient}
                        className="px-4 py-2 bg-blue-600 hover:bg-blue-500 rounded-lg text-sm font-semibold transition-all shadow-lg shadow-blue-500/20 flex items-center justify-center gap-2"
                    >
                        <span>+</span> Novo Cliente
                    </button>
                    <button onClick={() => auth.signOut()} className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg text-sm transition-all border border-gray-600">
                        Sair
                    </button>
                </div>
            </header>

            {/* Scrollable Content */}
            <div className="flex-1 overflow-y-auto p-6">
                <div className="max-w-7xl mx-auto">
                    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
                        <div className="overflow-x-auto">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                    <tr className="bg-gray-100 dark:bg-gray-700/50 text-gray-500 dark:text-gray-400 uppercase text-xs tracking-wider">
                                        <th
                                            className="px-6 py-4 font-medium cursor-pointer hover:text-gray-900 dark:hover:text-white transition-colors select-none"
                                            onClick={() => requestSort('NOME')}
                                        >
                                            <div className="flex items-center">Nome {getSortIcon('NOME')}</div>
                                        </th>
                                        <th
                                            className="px-6 py-4 font-medium cursor-pointer hover:text-gray-900 dark:hover:text-white transition-colors select-none"
                                            onClick={() => requestSort('USUARIO')}
                                        >
                                            <div className="flex items-center">Usuário {getSortIcon('USUARIO')}</div>
                                        </th>
                                        <th
                                            className="px-6 py-4 font-medium cursor-pointer hover:text-gray-900 dark:hover:text-white transition-colors select-none"
                                            onClick={() => requestSort('SITUACAO')}
                                        >
                                            <div className="flex items-center">Situação {getSortIcon('SITUACAO')}</div>
                                        </th>
                                        <th
                                            className="px-6 py-4 font-medium cursor-pointer hover:text-gray-900 dark:hover:text-white transition-colors select-none"
                                            onClick={() => requestSort('VENCIMENTO')}
                                        >
                                            <div className="flex items-center">Vencimento {getSortIcon('VENCIMENTO')}</div>
                                        </th>
                                        <th className="px-6 py-4 font-medium text-right">Ações</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                                    {filteredClientes.map((cliente) => (
                                        <tr
                                            key={cliente.id}
                                            onClick={() => router.push(`/clientes/${cliente.id}`)}
                                            className="hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors cursor-pointer"
                                        >
                                            <td className="px-6 py-4 font-medium text-gray-900 dark:text-white">{cliente.NOME}</td>
                                            <td className="px-6 py-4 text-gray-600 dark:text-gray-300">{cliente.USUARIO}</td>
                                            <td className="px-6 py-4">
                                                <span className={`px-3 py-1 rounded-full text-xs font-semibold border ${getStatusColor(cliente.SITUACAO)}`}>
                                                    {cliente.SITUACAO}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 text-gray-600 dark:text-gray-300">{cliente.VENCIMENTO}</td>
                                            <td className="px-6 py-4 text-right">
                                                <button
                                                    onClick={(e) => { e.stopPropagation(); openEditClient(cliente); }}
                                                    className="text-blue-400 hover:text-blue-300 mr-3 text-sm font-medium transition-colors"
                                                >
                                                    Editar
                                                </button>
                                                <button
                                                    onClick={(e) => { e.stopPropagation(); handleDelete(cliente.id); }}
                                                    className="text-red-400 hover:text-red-300 text-sm font-medium transition-colors"
                                                >
                                                    Excluir
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        {filteredClientes.length === 0 && (
                            <div className="p-8 text-center text-gray-500">Nenhum cliente encontrado com os filtros atuais.</div>
                        )}
                    </div>

                    {/* Confirmation Modal */}
                    {deleteConfirmation.isOpen && (
                        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
                            <div className="bg-white dark:bg-gray-800 p-6 rounded-xl w-full max-w-sm border border-gray-200 dark:border-gray-700 shadow-2xl">
                                <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-2">Confirmar Exclusão</h3>
                                <p className="text-gray-600 dark:text-gray-300 mb-6">
                                    Tem certeza que deseja excluir este cliente? Esta ação não pode ser desfeita.
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

                    <ClientFormModal
                        isOpen={isModalOpen}
                        onClose={() => setIsModalOpen(false)}
                        clienteToEdit={clientToEdit}
                    />
                </div>
            </div>
        </div>
    );
}
