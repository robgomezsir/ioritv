package com.elevadorcom.ioritv

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.elevadorcom.ioritv.utils.AccessibilityUtils

class IORITvApplication : Application() {

    companion object {
        lateinit var instance: IORITvApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Aplicar configurações de tema antes de qualquer Activity ser criada
        setupThemeSettings()
        
        // Aplicar configurações de acessibilidade se necessário
        setupAccessibilitySettings()
    }

    /**
     * Configurações iniciais de tema
     */
    private fun setupThemeSettings() {
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val themeMode = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    /**
     * Configurações iniciais de acessibilidade
     */
    private fun setupAccessibilitySettings() {
        // Verifica se alto contraste está ativo e aplica tema se necessário
        if (AccessibilityUtils.isHighContrastEnabled(this)) {
            AccessibilityUtils.applyHighContrastTheme(this, true)
        }
    }

    /**
     * Retorna o contexto da aplicação
     */
    fun getAppContext(): Context = applicationContext
}
