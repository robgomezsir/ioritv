package com.elevadorcom.ioritv.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.elevadorcom.ioritv.R
import com.google.android.material.color.DynamicColors

/**
 * Utilitario para gerenciar temas da aplicacao de forma centralizada.
 *
 * Suporta 4 modos:
 *   - MODE_FOLLOW_SYSTEM (0): segue o tema do dispositivo
 *   - MODE_LIGHT (1): tema claro
 *   - MODE_DARK (2): tema escuro
 *   - MODE_GLASS (4): glassmorphism (Aurora Frost)
 *
 * Dynamic Color (Material You): desativado por padrao.
 * Para ativar, chame DynamicColors.applyToActivitiesIfAvailable() no Application.
 *
 * Conforme PRD Papecon Office v1.0 Secao 2.
 */
object ThemeUtils {

    private const val PREFERENCES_NAME = "AppSettings"
    private const val THEME_MODE_KEY = "theme_mode"

    /**
     * Modo de tema Glassmorphism (valor proprio - nao e um modo do AppCompatDelegate).
     * Nao deve ser passado para AppCompatDelegate.setDefaultNightMode().
     */
    const val MODE_GLASS = 4

    /**
     * Aplica o tema apropriado para a Activity baseado nas preferencias do usuario.
     * Fluxo conforme PRD Secao 10: tema salvo -> resolucao -> MaterialTheme.
     */
    fun applyTheme(activity: AppCompatActivity) {
        val sharedPreferences = activity.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val themeMode = sharedPreferences.getInt(THEME_MODE_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> {
                activity.setTheme(R.style.Base_Theme_IORITv)
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                activity.setTheme(R.style.Base_Theme_IORITv_Dark)
            }
            MODE_GLASS -> {
                activity.setTheme(R.style.Base_Theme_IORITv_Glass)
            }
            else -> {
                // Modo automatico - segue o sistema
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
     * Salva o modo de tema selecionado pelo usuario
     */
    fun saveThemeMode(context: Context, mode: Int) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(THEME_MODE_KEY, mode)
        editor.apply()

        // Glass e um tema proprio, nao um modo noite do AppCompat
        if (mode != MODE_GLASS) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    /**
     * Recupera o modo de tema salvo nas preferencias
     */
    fun getSavedThemeMode(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(THEME_MODE_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    /**
     * Retorna o indice do tema atual para uso em dialogs
     */
    fun getCurrentThemeIndex(context: Context): Int {
        return when (getSavedThemeMode(context)) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> 0
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            MODE_GLASS -> 3
            else -> 0
        }
    }

    /**
     * Verifica se o tema atual e escuro.
     * No modo Glass, retorna false (usado para decidir a paleta de glass).
     */
    fun isDarkTheme(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val themeMode = sharedPreferences.getInt(THEME_MODE_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        return when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            MODE_GLASS -> false
            else -> {
                val isSystemDarkMode = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                isSystemDarkMode
            }
        }
    }

    /**
     * Verifica se o tema atual e claro
     */
    fun isLightTheme(context: Context): Boolean {
        return !isDarkTheme(context)
    }

    /**
     * Verifica se o modo Glass esta ativado
     */
    fun isGlassMode(context: Context): Boolean {
        return getSavedThemeMode(context) == MODE_GLASS
    }

    /**
     * Aplica Dynamic Colors (Material You) se disponivel (Android 12+).
     * Chamado no onCreate de Activities apos applyTheme().
     *
     * No PRD, Dynamic Color esta desativado por padrao (dynamicColor = false).
     * Esta funcionalidade pode ser habilitada via settings futuras.
     *
     * @param activity A Activity onde aplicar
     * @param enabled Se true, aplica dynamic colors quando disponivel
     */
    fun applyDynamicColorsIfAvailable(activity: AppCompatActivity, enabled: Boolean = false) {
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivityIfAvailable(activity)
        }
    }

    /**
     * Retorna a cor primaria resolvida para o tema atual (util para logs/debug).
     */
    fun getPrimaryColor(context: Context): Int {
        val typedValue = android.util.TypedValue()
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        return typedValue.data
    }
}
