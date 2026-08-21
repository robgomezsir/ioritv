import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.elevadorcom.ioritv.utils.SituacaoUtil
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class UpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        Log.d("UpdateWorker", "Worker iniciou")
        return try {
            atualizarSituacaoClientes()
            Log.d("UpdateWorker", "Worker finalizou com sucesso")
            Result.success()
        } catch (e: Exception) {
            Log.e("UpdateWorker", "Worker falhou", e)
            Result.failure()
        }
    }

    private suspend fun atualizarSituacaoClientes() {
        try {
            val result = db.collection("clientes").get().await()

            for (document in result) {
                val terminoTimestamp = document.getTimestamp("TERMINO")?.toDate()
                if (terminoTimestamp != null) {
                    val situacao = SituacaoUtil.calcularSituacao(terminoTimestamp)
                    val vencimento = calcularVencimento(terminoTimestamp)

                    val situacaoAtual = document.getString("SITUACAO") ?: ""
                    val vencimentoAtual = document.getString("VENCIMENTO") ?: ""

                    if (situacao != situacaoAtual || vencimento != vencimentoAtual) {
                        val clienteRef = db.collection("clientes").document(document.id)

                        clienteRef.update(
                            mapOf(
                                "SITUACAO" to situacao,
                                "VENCIMENTO" to vencimento
                            )
                        ).await()

                        Log.d("UpdateWorker", "Cliente ${document.id} atualizado para $situacao com vencimento '$vencimento'")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("UpdateWorker", "Falha ao recuperar ou atualizar clientes", e)
            throw e
        }
    }

    private fun calcularVencimento(termino: Date): String {
        val hoje = clearTime(Date())
        val terminoSemHora = clearTime(termino)
        val diasParaTermino = calculateDaysDifference(hoje, terminoSemHora)

        // Mesmas regras da Cloud Function calculateVencimentoString (fonte da verdade)
        return when {
            diasParaTermino > 2 -> "Faltam $diasParaTermino dias"
            diasParaTermino in 1..2 -> "Ainda falta(m) $diasParaTermino dia(s)"
            diasParaTermino == 0 -> "Vence hoje"
            diasParaTermino < 0 -> "Já são ${-diasParaTermino} dias vencidos"
            else -> "Faltam $diasParaTermino dias"
        }
    }

    private fun calculateDaysDifference(startDate: Date, endDate: Date): Int {
        val diffInMillis = endDate.time - startDate.time
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun clearTime(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
}