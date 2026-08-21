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

    // Regra canônica — Cloud Functions (calculateSituacao) é a fonte da verdade:
    // STANDBY (≤ −15), VENCIDO (−14..−1), A VENCER (0..2), ATIVO (≥ 3).
    // Mesmas janelas do SituacaoUtil.kt (app Android).
    if (diasParaVencimento <= -15) {
        return "STANDBY";
    }
    if (diasParaVencimento >= -14 && diasParaVencimento <= -1) {
        return "VENCIDO";
    }
    if (diasParaVencimento >= 0 && diasParaVencimento <= 2) {
        return "A VENCER";
    }
    return "ATIVO";
}
