package com.elevadorcom.ioritv

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Utilitário para limpar registros de despesas do Firebase Firestore
 * Este arquivo é destinado apenas para desenvolvimento/teste
 */
object ClearDespesasUtil {
    
    private const val TAG = "ClearDespesasUtil"
    
    /**
     * Limpa todos os registros de despesas do Firestore
     * ATENÇÃO: Esta operação é irreversível!
     */
    fun clearAllDespesas(onComplete: (success: Boolean, message: String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        
        Log.d(TAG, "Iniciando limpeza de despesas...")
        
        db.collection("despesas")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.d(TAG, "Nenhuma despesa encontrada")
                    onComplete(true, "Nenhuma despesa para excluir")
                    return@addOnSuccessListener
                }
                
                val batch = db.batch()
                var count = 0
                
                for (document in result) {
                    batch.delete(document.reference)
                    count++
                    Log.d(TAG, "Marcando despesa para exclusão: ${document.id}")
                }
                
                Log.d(TAG, "Executando batch delete de $count despesas...")
                
                batch.commit()
                    .addOnSuccessListener {
                        val message = "$count despesas excluídas com sucesso!"
                        Log.d(TAG, message)
                        onComplete(true, message)
                    }
                    .addOnFailureListener { e ->
                        val message = "Erro ao executar batch delete: ${e.message}"
                        Log.e(TAG, message, e)
                        onComplete(false, message)
                    }
            }
            .addOnFailureListener { e ->
                val message = "Erro ao buscar despesas: ${e.message}"
                Log.e(TAG, message, e)
                onComplete(false, message)
            }
    }
    
    /**
     * Conta o número de despesas registradas
     */
    fun countDespesas(onComplete: (count: Int) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        
        db.collection("despesas")
            .get()
            .addOnSuccessListener { result ->
                val count = result.size()
                Log.d(TAG, "Total de despesas: $count")
                onComplete(count)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao contar despesas: ${e.message}", e)
                onComplete(0)
            }
    }
}

