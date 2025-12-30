import { Timestamp } from "firebase/firestore";

export interface Cliente {
    SITUACAO: string;
    TERMINO?: Timestamp;
    VALOR?: number;
    [key: string]: any;
}

export function calculateDaysDifference(date1: Date, date2: Date): number {
    const d1 = new Date(date1);
    const d2 = new Date(date2);
    d1.setHours(0, 0, 0, 0);
    d2.setHours(0, 0, 0, 0);
    const diffTime = d2.getTime() - d1.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
}

export function getSmartStatus(cliente: Cliente): string {
    const situacao = cliente.SITUACAO || "";
    const termino = cliente.TERMINO;

    if (!termino) return situacao;

    const hoje = new Date();
    const terminoDate = termino.toDate();
    const diasParaVencimento = calculateDaysDifference(hoje, terminoDate);

    // Alinhado com MainActivity4.kt do Android
    if (situacao === "STANDBY" || diasParaVencimento <= -30) {
        return "STANDBY";
    }
    if (situacao === "VENCIDO" || (diasParaVencimento <= -15 && diasParaVencimento > -30)) {
        return "VENCIDO";
    }
    if (situacao === "A VENCER" || (diasParaVencimento >= 0 && diasParaVencimento <= 3)) {
        return "A VENCER";
    }
    if (situacao === "ATIVO" || diasParaVencimento > 3) {
        return "ATIVO";
    }

    return situacao;
}
