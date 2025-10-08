package com.elevadorcom.ioritv.utils

import android.content.Context
import com.elevadorcom.ioritv.BuildConfig
import com.elevadorcom.ioritv.R

/**
 * Utilitário para gerenciar informações de versão do aplicativo
 */
object VersionUtils {
    
    /**
     * Retorna o nome da versão do aplicativo (ex: "4.2")
     */
    fun getVersionName(): String {
        return BuildConfig.VERSION_NAME
    }
    
    /**
     * Retorna o código da versão do aplicativo (ex: 42)
     */
    fun getVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }
    
    /**
     * Retorna a versão formatada (ex: "v4.2")
     */
    fun getFormattedVersion(context: Context): String {
        return context.getString(R.string.app_version, getVersionName())
    }
    
    /**
     * Retorna o texto completo do rodapé com a versão
     */
    fun getFooterText(context: Context): String {
        return context.getString(R.string.footer_text, getVersionName())
    }
    
    /**
     * Retorna informações completas da versão para debug
     */
    fun getVersionInfo(): String {
        return "Version ${getVersionName()} (${getVersionCode()})"
    }
}

