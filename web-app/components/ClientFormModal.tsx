"use client";

import { useState, useEffect } from "react";
import { addDoc, collection, doc, updateDoc, Timestamp } from "firebase/firestore";
import { db } from "@/firebase/config";

interface Cliente {
    id?: string;
    NOME: string;
    USUARIO: string;
    SENHA?: string;
    WHATSAPP?: string;
    MODELO?: string;
    INICIO?: Timestamp | null;
    CREDITOS: number;
    MAC?: string;
    OTP?: string;
    DEVICE?: string;
    VALOR?: number;
    CUSTO?: number;
    DESCONTO?: number;
    SERVIDOR?: string;
    // Computed fields (readonly in form usually)
    SITUACAO?: string;
    VENCIMENTO?: string;
    TERMINO?: Timestamp | null;
}

interface Props {
    isOpen: boolean;
    onClose: () => void;
    clienteToEdit?: Cliente | null;
}

export default function ClientFormModal({ isOpen, onClose, clienteToEdit }: Props) {
    const [formData, setFormData] = useState<Partial<Cliente>>({
        NOME: "",
        USUARIO: "",
        SENHA: "",
        CREDITOS: 1,
        VALOR: 25.0,
        CUSTO: 0,
        DESCONTO: 0,
        // defaults
    });
    const [inicioDate, setInicioDate] = useState("");
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (clienteToEdit) {
            setFormData(clienteToEdit);
            // Convert Timestamp to YYYY-MM-DD for input
            if (clienteToEdit.INICIO) {
                const date = clienteToEdit.INICIO.toDate ? clienteToEdit.INICIO.toDate() : new Date(clienteToEdit.INICIO.seconds * 1000);
                setInicioDate(date.toISOString().split('T')[0]);
            } else {
                setInicioDate("");
            }
        } else {
            setFormData({
                NOME: "",
                USUARIO: "",
                SENHA: "",
                CREDITOS: 1,
                VALOR: 25.0,
                CUSTO: 0,
                DESCONTO: 0,
                WHATSAPP: "",
                MODELO: "",
                MAC: "",
                OTP: "",
                DEVICE: "",
                SERVIDOR: "",
            });
            setInicioDate(new Date().toISOString().split('T')[0]); // Default to today
        }
    }, [clienteToEdit, isOpen]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value, type } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'number' ? parseFloat(value) : value
        }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);

        try {
            const dataToSave = {
                ...formData,
                INICIO: inicioDate ? Timestamp.fromDate(new Date(inicioDate + "T12:00:00")) : null,
                // Ensure numbers are numbers
                CREDITOS: Number(formData.CREDITOS),
                VALOR: Number(formData.VALOR),
                CUSTO: Number(formData.CUSTO),
                DESCONTO: Number(formData.DESCONTO),
            };

            if (clienteToEdit?.id) {
                // Update
                const docRef = doc(db, "clientes", clienteToEdit.id);
                const { id: _id, ...updateData } = dataToSave as Cliente; // Remove id from data
                void _id;
                await updateDoc(docRef, updateData);
            } else {
                // Create
                // Initial SITUACAO/TERMINO will be calculated by Cloud Function
                await addDoc(collection(db, "clientes"), dataToSave);
            }
            onClose();
        } catch (error) {
            console.error("Error saving client:", error);
            alert("Erro ao salvar cliente.");
        } finally {
            setLoading(false);
        }
    };

    const inputClass = "w-full bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg px-3 py-2 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 outline-none transition-all";
    const labelClass = "text-sm text-gray-500 dark:text-gray-400 block mb-1";

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
            <div className="bg-white dark:bg-gray-800 rounded-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto border border-gray-200 dark:border-gray-700 shadow-2xl">
                <div className="p-6 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center bg-gray-50 dark:bg-gray-800/50 sticky top-0 z-10">
                    <h2 className="text-xl font-bold text-gray-900 dark:text-white">
                        {clienteToEdit ? "Editar Cliente" : "Novo Cliente"}
                    </h2>
                    <button onClick={onClose} className="text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors text-2xl leading-none">&times;</button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-4">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className={labelClass}>Nome</label>
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
                            <input name="WHATSAPP" value={formData.WHATSAPP || ""} onChange={handleChange} className={inputClass} />
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div>
                            <label className={labelClass}>Inicio</label>
                            <input type="date" value={inicioDate} onChange={(e) => setInicioDate(e.target.value)} required className={inputClass} />
                        </div>
                        <div>
                            <label className={labelClass}>Créditos (Meses)</label>
                            <input type="number" name="CREDITOS" value={formData.CREDITOS} onChange={handleChange} required className={inputClass} />
                        </div>
                        <div>
                            <label className={labelClass}>Valor</label>
                            <input type="number" step="0.01" name="VALOR" value={formData.VALOR} onChange={handleChange} className={inputClass} />
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div>
                            <label className={labelClass}>Custo</label>
                            <input type="number" step="0.01" name="CUSTO" value={formData.CUSTO || 0} onChange={handleChange} className={inputClass} />
                        </div>
                        <div>
                            <label className={labelClass}>Desconto</label>
                            <input type="number" step="0.01" name="DESCONTO" value={formData.DESCONTO || 0} onChange={handleChange} className={inputClass} />
                        </div>
                        <div>
                            <label className={labelClass}>Servidor</label>
                            <input name="SERVIDOR" value={formData.SERVIDOR || ""} onChange={handleChange} className={inputClass} />
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className={labelClass}>MAC</label>
                            <input name="MAC" value={formData.MAC || ""} onChange={handleChange} className={inputClass} />
                        </div>
                        <div>
                            <label className={labelClass}>Device</label>
                            <input name="DEVICE" value={formData.DEVICE || ""} onChange={handleChange} className={inputClass} />
                        </div>
                    </div>

                    <div className="pt-6 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3 sticky bottom-0 bg-white dark:bg-gray-800 pb-2">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors font-medium"
                        >
                            Cancelar
                        </button>
                        <button
                            type="submit"
                            disabled={loading}
                            className="px-6 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-lg font-medium shadow-lg hover:shadow-blue-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {loading ? "Salvando..." : "Salvar Cliente"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
