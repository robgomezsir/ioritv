package com.elevadorcom.ioritv.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

/**
 * Gerenciador de sessão do usuário
 * Mantém o estado de autenticação persistente entre reinicializações do app
 */
class SessionManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "SessionManager"
        private const val PREFS_NAME = "login_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_LAST_LOGIN = "last_login_time"
        private const val KEY_KEEP_LOGGED_IN = "keep_logged_in_preference"
        private const val SESSION_TIMEOUT = 30L * 24 * 60 * 60 * 1000 // 30 dias em ms
        
        @Volatile
        private var instance: SessionManager? = null
        
        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Salva o estado de login do usuário
     */
    fun saveLoginStatus() {
        val user = auth.currentUser
        if (user != null) {
            sharedPreferences.edit().apply {
                putBoolean(KEY_IS_LOGGED_IN, true)
                putString(KEY_USER_EMAIL, user.email)
                putString(KEY_USER_ID, user.uid)
                putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
                apply()
            }
            Log.d(TAG, "Login status saved for user: ${user.email}")
        }
    }
    
    /**
     * Verifica se o usuário está logado
     * @return true se o usuário está logado e a sessão é válida
     */
    fun isUserLoggedIn(): Boolean {
        val isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        val lastLoginTime = sharedPreferences.getLong(KEY_LAST_LOGIN, 0)
        val currentTime = System.currentTimeMillis()
        
        // Verifica se a sessão expirou
        val sessionExpired = (currentTime - lastLoginTime) > SESSION_TIMEOUT
        
        if (isLoggedIn && !sessionExpired && auth.currentUser != null) {
            val savedEmail = sharedPreferences.getString(KEY_USER_EMAIL, "")
            val currentEmail = auth.currentUser?.email
            
            // Verifica se o email salvo corresponde ao usuário atual
            if (savedEmail == currentEmail && auth.currentUser?.isEmailVerified == true) {
                // Atualiza o timestamp do último login
                sharedPreferences.edit().putLong(KEY_LAST_LOGIN, currentTime).apply()
                return true
            } else {
                // Email não corresponde ou não verificado, limpa o login
                clearLoginStatus()
            }
        } else if (sessionExpired) {
            // Sessão expirada, limpa o login
            clearLoginStatus()
        }
        
        return false
    }
    
    /**
     * Limpa o estado de login do usuário
     */
    fun clearLoginStatus() {
        sharedPreferences.edit().apply {
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_ID)
            remove(KEY_LAST_LOGIN)
            apply()
        }
        
        // Faz logout do Firebase também
        auth.signOut()
        
        Log.d(TAG, "Login status cleared")
    }
    
    /**
     * Retorna o email do usuário logado
     */
    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }
    
    /**
     * Retorna o ID do usuário logado
     */
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }
    
    /**
     * Retorna o tempo em dias desde o último login
     */
    fun getDaysSinceLastLogin(): Long {
        val lastLoginTime = sharedPreferences.getLong(KEY_LAST_LOGIN, 0)
        val currentTime = System.currentTimeMillis()
        val diffMs = currentTime - lastLoginTime
        return diffMs / (24 * 60 * 60 * 1000)
    }
    
    /**
     * Verifica se a sessão está próxima de expirar (últimos 3 dias)
     */
    fun isSessionNearExpiry(): Boolean {
        val daysSinceLogin = getDaysSinceLastLogin()
        return daysSinceLogin >= 27 // Alerta nos últimos 3 dias
    }
    
    /**
     * Salva a preferência do usuário sobre manter-se logado
     */
    fun setKeepLoggedInPreference(keepLoggedIn: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean(KEY_KEEP_LOGGED_IN, keepLoggedIn)
            apply()
        }
        Log.d(TAG, "Keep logged in preference saved: $keepLoggedIn")
    }
    
    /**
     * Retorna a preferência do usuário sobre manter-se logado
     * @return true se o usuário quer manter-se logado (padrão: true)
     */
    fun getKeepLoggedInPreference(): Boolean {
        return sharedPreferences.getBoolean(KEY_KEEP_LOGGED_IN, true)
    }
}

