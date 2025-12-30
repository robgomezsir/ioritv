const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { addMonths, differenceInDays, startOfDay } = require("date-fns");

admin.initializeApp();
const db = admin.firestore();

const CONSTANTS = {
    ATIVO: "ATIVO",
    A_VENCER: "A VENCER",
    VENCIDO: "VENCIDO",
    STANDBY: "STANDBY"
};

/**
 * Helper to calculate termination date
 */
const calculateTermino = (inicioDate, creditos) => {
    if (!inicioDate) return new Date();
    return addMonths(inicioDate, creditos);
};

/**
 * Helper to calculate situation based on termination date
 */
const calculateSituacao = (terminoDate) => {
    const hoje = startOfDay(new Date());
    const termino = startOfDay(terminoDate);
    const diasRestantes = differenceInDays(termino, hoje);

    if (diasRestantes <= -15) return CONSTANTS.STANDBY;
    if (diasRestantes >= -14 && diasRestantes <= -1) return CONSTANTS.VENCIDO;
    if (diasRestantes >= 0 && diasRestantes <= 2) return CONSTANTS.A_VENCER;
    return CONSTANTS.ATIVO;
};

/**
 * Helper to calculate vencimento string
 */
const calculateVencimentoString = (terminoDate) => {
    const hoje = startOfDay(new Date());
    const termino = startOfDay(terminoDate);
    const diasRestantes = differenceInDays(termino, hoje);

    if (diasRestantes > 2) return `Faltam ${diasRestantes} dias`;
    if (diasRestantes >= 1 && diasRestantes <= 2) return `Ainda falta(m) ${diasRestantes} dia(s)`;
    if (diasRestantes === 0) return "Vence hoje";
    if (diasRestantes < 0) return `Já são ${Math.abs(diasRestantes)} dias vencidos`;

    // Fallback? Should be covered above, but just in case
    return `Faltam ${diasRestantes} dias`;
};

exports.onClienteWrite = functions.firestore
    .document("clientes/{clienteId}")
    .onWrite(async (change, context) => {
        const newData = change.after.exists ? change.after.data() : null;
        const oldData = change.before.exists ? change.before.data() : null;

        if (!newData) return null; // Deleted document

        let updates = {};

        // Recalculate TERMINO if INICIO or CREDITOS changed
        // Note: Firestore timestamps need conversion
        const inicioTimestamp = newData.INICIO;
        const creditos = newData.CREDITOS || 0;

        let terminoDate = newData.TERMINO ? newData.TERMINO.toDate() : null;

        const inicioChanged = !oldData || (oldData.INICIO && !newData.INICIO) || (oldData.INICIO && newData.INICIO && !oldData.INICIO.isEqual(newData.INICIO));
        const creditosChanged = !oldData || oldData.CREDITOS !== newData.CREDITOS;

        if (inicioChanged || creditosChanged) {
            if (inicioTimestamp) {
                terminoDate = calculateTermino(inicioTimestamp.toDate(), creditos);
                updates.TERMINO = admin.firestore.Timestamp.fromDate(terminoDate);
            }
        }

        // Always recalculate SITUACAO and VENCIMENTO if TERMINO is present
        if (terminoDate) {
            const newSituacao = calculateSituacao(terminoDate);
            const newVencimento = calculateVencimentoString(terminoDate);

            if (newSituacao !== newData.SITUACAO) {
                updates.SITUACAO = newSituacao;
            }
            if (newVencimento !== newData.VENCIMENTO) {
                updates.VENCIMENTO = newVencimento;
            }
        }

        // Avoid infinite loops: only update if there are changes
        if (Object.keys(updates).length > 0) {
            console.log(`Updating client ${context.params.clienteId}:`, updates);
            return change.after.ref.update(updates);
        }

        return null;
    });

exports.checkSituacaoDaily = functions.pubsub.schedule("every day 00:00")
    .timeZone("America/Sao_Paulo")
    .onRun(async (context) => {
        const snapshot = await db.collection("clientes").get();
        const batch = db.batch();
        let updateCount = 0;

        snapshot.forEach(doc => {
            const data = doc.data();
            if (!data.TERMINO) return;

            const terminoDate = data.TERMINO.toDate();
            const newSituacao = calculateSituacao(terminoDate);
            const newVencimento = calculateVencimentoString(terminoDate);

            let updates = {};
            if (newSituacao !== data.SITUACAO) updates.SITUACAO = newSituacao;
            if (newVencimento !== data.VENCIMENTO) updates.VENCIMENTO = newVencimento;

            if (Object.keys(updates).length > 0) {
                batch.update(doc.ref, updates);
                updateCount++;
            }
        });

        if (updateCount > 0) {
            await batch.commit();
            console.log(`Updated ${updateCount} clients via daily check.`);
        } else {
            console.log("No clients needed updates.");
        }
    });
