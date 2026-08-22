"use client";

import { useEffect, useState } from "react";
import { collection, addDoc, doc, updateDoc } from "firebase/firestore";
import { db } from "@//firebase/config";

interface Despesa {
    id?: string;
    data: string;
    descricao: string;
    detalhes?: string;
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
    const [titulo, setTitulo] = useState("");
    const [descricao, setDescricao] = useState("");
    const [valorStr, setValorStr] = useState("");
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (isOpen) {
            if (despesaToEdit) {
                setDataStr(despesaToEdit.data);
                setTitulo(despesaToEdit.descricao);
                setDescricao(despesaToEdit.detalhes || "");
                setValorStr(despesaToEdit.valor.toFixed(2));
            } else {
                const today = new Date();
                const dd = String(today.getDate()).padStart(2, '0');
                const mm = String(today.getMonth() + 1).padStart(2, '0');
                const yyyy = today.getFullYear();
                setDataStr(`${dd}/${mm}/${yyyy}`);
                setTitulo("");
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
                descricao: titulo,
                detalhes: descricao,
                valor,
                dataTimestamp: Date.now()
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
            <div className="glass-modal max-w-md w-full overflow-hidden">
                <div className="px-6 py-4 border-b border-white/5">
                    <h2 className="text-xl font-bold text-[var(--on-surface)]">
                        {despesaToEdit ? "Editar Despesa" : "Nova Despesa"}
                    </h2>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-4">
                    {/* Data */}
                    <div>
                        <label className="block text-xs font-semibold text-[var(--on-surface-variant)] mb-1.5 uppercase tracking-wider">
                            Data
                        </label>
                        <input
                            type="text"
                            required
                            placeholder="dd/mm/aaaa"
                            className="glass-input w-full"
                            value={dataStr}
                            onChange={(e) => setDataStr(e.target.value)}
                        />
                    </div>

                    {/* Título */}
                    <div>
                        <label className="block text-xs font-semibold text-[var(--on-surface-variant)] mb-1.5 uppercase tracking-wider">
                            Título da Despesa
                        </label>
                        <input
                            type="text"
                            required
                            placeholder="Ex: Aluguel, Internet, Equipamento..."
                            className="glass-input w-full"
                            value={titulo}
                            onChange={(e) => setTitulo(e.target.value)}
                        />
                    </div>

                    {/* Descrição (multiline) */}
                    <div>
                        <label className="block text-xs font-semibold text-[var(--on-surface-variant)] mb-1.5 uppercase tracking-wider">
                            Descrição
                        </label>
                        <textarea
                            rows={4}
                            placeholder="Detalhes sobre esta despesa..."
                            className="glass-input w-full resize-y min-h-[100px]"
                            value={descricao}
                            onChange={(e) => setDescricao(e.target.value)}
                        />
                    </div>

                    {/* Valor */}
                    <div>
                        <label className="block text-xs font-semibold text-[var(--on-surface-variant)] mb-1.5 uppercase tracking-wider">
                            Valor (R$)
                        </label>
                        <input
                            type="number"
                            step="0.01"
                            required
                            placeholder="0,00"
                            className="glass-input w-full"
                            value={valorStr}
                            onChange={(e) => setValorStr(e.target.value)}
                        />
                    </div>

                    {/* Botões */}
                    <div className="flex gap-3 pt-4 border-t border-white/5">
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 glass-input text-center text-sm cursor-pointer"
                        >
                            Cancelar
                        </button>
                        <button
                            type="submit"
                            disabled={loading}
                            className="flex-1 glass-btn-primary text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {loading ? "Salvando..." : "Salvar"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
