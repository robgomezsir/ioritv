"use client";

import { useEffect, useState } from "react";
import { collection, addDoc, doc, updateDoc, Timestamp } from "firebase/firestore";
import { db } from "@/firebase/config";

interface Despesa {
    id?: string;
    data: string; // Stored as string dd/MM/yyyy in Android, let's keep consistency or upgrade to ISO? Android saves as string "data" and timestamp "dataTimestamp".
    descricao: string;
    valor: number;
    dataTimestamp?: number;
}

interface DespesaModalProps {
    isOpen: boolean;
    onClose: () => void;
    despesaToEdit?: Despesa | null;
    onSuccess: () => void;
}

export default function DespesaModal({ isOpen, onClose, despesaToEdit, onSuccess }: DespesaModalProps) {
    const [dataStr, setDataStr] = useState("");
    const [descricao, setDescricao] = useState("");
    const [valorStr, setValorStr] = useState("");
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (isOpen) {
            if (despesaToEdit) {
                setDataStr(despesaToEdit.data);
                setDescricao(despesaToEdit.descricao);
                setValorStr(despesaToEdit.valor.toFixed(2));
            } else {
                // Default to today
                const today = new Date();
                const dd = String(today.getDate()).padStart(2, '0');
                const mm = String(today.getMonth() + 1).padStart(2, '0');
                const yyyy = today.getFullYear();
                setDataStr(`${dd}/${mm}/${yyyy}`);
                setDescricao("");
                setValorStr("");
            }
        }
    }, [isOpen, despesaToEdit]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);

        try {
            const valor = parseFloat(valorStr.replace(",", "."));
            if (isNaN(valor)) throw new Error("Valor inválido");

            const despesaData = {
                data: dataStr,
                descricao,
                valor,
                dataTimestamp: Date.now() // Simple timestamp for sorting
            };

            if (despesaToEdit?.id) {
                await updateDoc(doc(db, "despesas", despesaToEdit.id), despesaData);
            } else {
                await addDoc(collection(db, "despesas"), despesaData);
            }

            onSuccess();
            onClose();
        } catch (error) {
            console.error("Error saving despesa:", error);
            alert("Erro ao salvar despesa");
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
            <div className="bg-white dark:bg-gray-800 rounded-xl max-w-md w-full border border-gray-200 dark:border-gray-700 shadow-2xl overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
                    <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                        {despesaToEdit ? "Editar Despesa" : "Nova Despesa"}
                    </h2>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-400 mb-1">Data (dd/mm/aaaa)</label>
                        <input
                            type="text"
                            required
                            placeholder="dd/mm/aaaa"
                            className="w-full bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg px-4 py-2 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 outline-none transition-all"
                            value={dataStr}
                            onChange={(e) => setDataStr(e.target.value)}
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-400 mb-1">Descrição</label>
                        <input
                            type="text"
                            required
                            className="w-full bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg px-4 py-2 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 outline-none transition-all"
                            value={descricao}
                            onChange={(e) => setDescricao(e.target.value)}
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-400 mb-1">Valor (R$)</label>
                        <input
                            type="number"
                            step="0.01"
                            required
                            className="w-full bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg px-4 py-2 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 outline-none transition-all"
                            value={valorStr}
                            onChange={(e) => setValorStr(e.target.value)}
                        />
                    </div>

                    <div className="flex gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 px-4 py-2 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 rounded-lg text-gray-700 dark:text-gray-200 font-medium transition-colors"
                        >
                            Cancelar
                        </button>
                        <button
                            type="submit"
                            disabled={loading}
                            className="flex-1 px-4 py-2 bg-blue-600 hover:bg-blue-500 rounded-lg text-white font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-lg hover:shadow-blue-500/25"
                        >
                            {loading ? "Salvando..." : "Salvar"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
