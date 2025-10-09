package com.elevadorcom.ioritv.utils

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * Utilitário para diagnóstico de problemas com Firebase Authentication
 * Especialmente para recuperação de senha
 */
object FirebaseAuthDiagnostics {
    
    private const val TAG = "FirebaseAuthDiagnostics"
    
    /**
     * Executa diagnóstico completo do Firebase Auth
     */
    fun runDiagnostics(context: Context) {
        Log.d(TAG, "=== INICIANDO DIAGNÓSTICO FIREBASE AUTH ===")
        
        // 1. Verificar configuração do Firebase
        checkFirebaseConfiguration()
        
        // 2. Verificar usuário atual
        checkCurrentUser()
        
        // 3. Verificar configurações de email
        checkEmailSettings()
        
        // 4. Verificar conectividade
        checkConnectivity(context)
        
        Log.d(TAG, "=== DIAGNÓSTICO CONCLUÍDO ===")
    }
    
    private fun checkFirebaseConfiguration() {
        Log.d(TAG, "--- Verificando configuração do Firebase ---")
        
        try {
            val auth = FirebaseAuth.getInstance()
            val app = auth.app
            
            Log.d(TAG, "✓ Firebase App Name: ${app.name}")
            Log.d(TAG, "✓ Firebase App Options: ${app.options.projectId}")
            Log.d(TAG, "✓ Firebase Auth Instance: ${auth.app.name}")
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Erro na configuração do Firebase: ${e.message}", e)
        }
    }
    
    private fun checkCurrentUser() {
        Log.d(TAG, "--- Verificando usuário atual ---")
        
        try {
            val auth = FirebaseAuth.getInstance()
            val user: FirebaseUser? = auth.currentUser
            
            if (user != null) {
                Log.d(TAG, "✓ Usuário logado: ${user.email}")
                Log.d(TAG, "✓ Email verificado: ${user.isEmailVerified}")
                Log.d(TAG, "✓ UID: ${user.uid}")
                Log.d(TAG, "✓ Provider: ${user.providerId}")
            } else {
                Log.d(TAG, "ℹ Nenhum usuário logado atualmente")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Erro ao verificar usuário: ${e.message}", e)
        }
    }
    
    private fun checkEmailSettings() {
        Log.d(TAG, "--- Verificando configurações de email ---")
        
        try {
            val auth = FirebaseAuth.getInstance()
            
            // Verificar se o Auth está configurado corretamente
            Log.d(TAG, "✓ Firebase Auth configurado: ${auth.app.name}")
            
            // Log de configurações que podem afetar o envio de emails
            Log.d(TAG, "ℹ Configurações importantes:")
            Log.d(TAG, "  - Domain autorizado deve estar configurado no Firebase Console")
            Log.d(TAG, "  - SMTP deve estar habilitado no Firebase Console")
            Log.d(TAG, "  - Templates de email devem estar configurados")
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Erro ao verificar configurações de email: ${e.message}", e)
        }
    }
    
    private fun checkConnectivity(context: Context) {
        Log.d(TAG, "--- Verificando conectividade ---")
        
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            if (networkCapabilities != null) {
                val hasInternet = networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val hasValidated = networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                
                Log.d(TAG, "✓ Conectividade com internet: $hasInternet")
                Log.d(TAG, "✓ Rede validada: $hasValidated")
            } else {
                Log.w(TAG, "⚠ Nenhuma rede ativa detectada")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Erro ao verificar conectividade: ${e.message}", e)
        }
    }
    
    /**
     * Lista de verificação para problemas comuns de recuperação de senha
     */
    fun printTroubleshootingGuide() {
        Log.d(TAG, "=== GUIA DE SOLUÇÃO DE PROBLEMAS ===")
        Log.d(TAG, "1. Verifique se o email existe no Firebase Auth")
        Log.d(TAG, "2. Confirme se o domínio está autorizado no Firebase Console")
        Log.d(TAG, "3. Verifique a pasta de spam/lixo eletrônico")
        Log.d(TAG, "4. Confirme se o SMTP está habilitado no Firebase Console")
        Log.d(TAG, "5. Verifique se os templates de email estão configurados")
        Log.d(TAG, "6. Teste com diferentes provedores de email")
        Log.d(TAG, "7. Verifique logs do Firebase Console para erros")
        Log.d(TAG, "8. Confirme se não há bloqueios de firewall")
        Log.d(TAG, "=== FIM DO GUIA ===")
    }
}
