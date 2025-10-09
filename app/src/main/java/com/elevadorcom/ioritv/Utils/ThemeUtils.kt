package com.elevadorcom.ioritv.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.elevadorcom.ioritv.R

/**
 * Utilitário para gerenciar temas da aplicação de forma centralizada
 */
object ThemeUtils {

    private const val PREFERENCES_NAME = "AppSettings"
    private const val THEME_MODE_KEY = "theme_mode"

    /**
     * Aplica o tema apropriado para a Activity baseado nas preferências do usuário
     */
    fun applyTheme(activity: AppCompatActivity) {
        val sharedPreferences = activity.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val themeMode = sharedPreferences.getInt(THEME_MODE_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        // Aplica o tema baseado no modo selecionado
        when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> {
                // Modo claro
                activity.setTheme(R.style.Base_Theme_IORITv)
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                // Modo escuro
                activity.setTheme(R.style.Base_Theme_IORITv_Dark)
            }
            else -> {
                // Modo automático - segue o sistema
                val isSystemDarkMode = activity.resources.configuration.uiMode and 
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK == 
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                
                if (isSystemDarkMode) {
                    activity.setTheme(R.style.Base_Theme_IORITv_Dark)
                } else {
                    activity.setTheme(R.style.Base_Theme_IORITv)
                }
            }
        }
    }

    /**
     * Salva o modo de tema selecionado pelo usuário
     */
    fun saveThemeMode(context: Context, mode: Int) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(THEME_MODE_KEY, mode)
        editor.apply()
        
        // Aplica o novo tema imediatamente
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Recupera o modo de tema salvo nas preferências
     */
    fun getSavedThemeMode(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(THEME_MODE_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    /**
     * Retorna o índice do tema atual para uso em dialogs
     */
    fun getCurrentThemeIndex(context: Context): Int {
        return when (getSavedThemeMode(context)) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> 0 // Automático
            AppCompatDelegate.MODE_NIGHT_NO -> 1 // Claro
            AppCompatDelegate.MODE_NIGHT_YES -> 2 // Escuro
            else -> 0
        }
    }

    /**
     * Verifica se o tema atual é escuro
     */
    fun isDarkTheme(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val themeMode = sharedPreferences.getInt(THEME_MODE_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        return when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> {
                // Modo automático - verifica o sistema
                val isSystemDarkMode = context.resources.configuration.uiMode and 
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK == 
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                isSystemDarkMode
            }
        }
    }

    /**
     * Verifica se o tema atual é claro
     */
    fun isLightTheme(context: Context): Boolean {
        return !isDarkTheme(context)
    }
}