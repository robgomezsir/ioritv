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
 * Suporta 3 modos de tema base:
 *   - MODE_FOLLOW_SYSTEM (0): segue o tema do dispositivo
 *   - MODE_LIGHT (1): tema claro
 *   - MODE_DARK (2): tema escuro
 *
 * E 1 skin overlay opcional:
 *   - glass_enabled (boolean): efeito Glassmorphism (Aurora Frost)
 *     aplicado SOBRE o tema base (claro ou escuro).
 *
 * Dynamic Color (Material You): desativado por padrao.
 *
 * Conforme PRD Papecon Office v1.0 Secao 2.
 */
object ThemeUtils {

    private const val PREFERENCES_NAME = "AppSettings"
    private const val THEME_MODE_KEY = "theme_mode"
    private const val GLASS_ENABLED_KEY = "glass_enabled"

    /**
     * Valor antigo do modo Glass (legado). Usado apenas para migração.
     */
    const val MODE_GLASS = 4

    /**
     * Aplica o tema base apropriado para a Activity.
     * A skin Glass (se ativa) e aplicada separadamente nas Activities.
     */
    fun applyTheme(activity: AppCompatActivity) {
        // Migração: theme_mode antigo (4 = Glass) → theme_mode=1 + glass_enabled=true
        migrateLegacyGlass(activity)

        val sharedPreferences = activity.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val themeMode = sharedPreferences.getInt(THEME_MODE_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> {
                activity.setTheme(R.style.Base_Theme_IORITv)
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                activity.setTheme(R.style.Base_Theme_IORITv_Dark)
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
     * Migra theme_mode antigo (4 = Glass) para a nova estrutura:
     * theme_mode = 1 (LIGHT) + glass_enabled = true
     */
    private fun migrateLegacyGlass(context: Context) {
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val currentMode = prefs.getInt(THEME_MODE_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (currentMode == MODE_GLASS) {
            prefs.edit()
                .putInt(THEME_MODE_KEY, AppCompatDelegate.MODE_NIGHT_NO) // base = claro
                .putBoolean(GLASS_ENABLED_KEY, true)
                .apply()
        }
    }

    /**
     * Salva o modo de tema base selecionado pelo usuario.
     * Nao altera o estado do glass_enabled.
     */
    fun saveThemeMode(context: Context, mode: Int) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(THEME_MODE_KEY, mode).apply()

        // Glass e uma skin overlay, nao um modo noite do AppCompat
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Recupera o modo de tema base salvo
     */
    fun getSavedThemeMode(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(THEME_MODE_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    // ═══════════════════════════════════════════════════════════════
    // Glass — Skin overlay (ligar/desligar sobre o tema base)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Retorna true se a skin Glass esta ativada.
     */
    fun isGlassEnabled(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(GLASS_ENABLED_KEY, false)
    }

    /**
     * Liga ou desliga a skin Glass.
     * Chame recreate() na Activity apos alterar.
     */
    fun setGlassEnabled(context: Context, enabled: Boolean) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(GLASS_ENABLED_KEY, enabled).apply()
    }

    /**
     * Verifica se o tema atual e escuro (base, ignorando glass).
     */
    fun isDarkTheme(context: Context): Boolean {
        val themeMode = getSavedThemeMode(context)
        return when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> {
                context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    /**
     * Verifica se o tema atual e claro (base, ignorando glass).
     */
    fun isLightTheme(context: Context): Boolean {
        return !isDarkTheme(context)
    }

    /**
     * Retorna o indice do tema atual para uso em dialogs (0=auto, 1=light, 2=dark).
     */
    fun getCurrentThemeIndex(context: Context): Int {
        return when (getSavedThemeMode(context)) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> 0
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        }
    }

    /**
     * Aplica Dynamic Colors (Material You) se disponivel (Android 12+).
     */
    fun applyDynamicColorsIfAvailable(activity: AppCompatActivity, enabled: Boolean = false) {
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivityIfAvailable(activity)
        }
    }

    /**
     * Retorna a cor primaria resolvida para o tema atual.
     */
    fun getPrimaryColor(context: Context): Int {
        val typedValue = android.util.TypedValue()
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        return typedValue.data
    }
}
