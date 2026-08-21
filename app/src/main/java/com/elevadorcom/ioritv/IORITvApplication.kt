package com.elevadorcom.ioritv

import UpdateWorker
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.elevadorcom.ioritv.utils.AccessibilityUtils
import com.elevadorcom.ioritv.utils.ThemeUtils
import com.onesignal.OneSignal
import java.util.Calendar
import java.util.concurrent.TimeUnit

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

        // Agendamento único do WorkManager (diário) — centralizado aqui (Fase 6)
        setupDailyWork()

        // Inicialização única do OneSignal — centralizada aqui (Fase 6)
        setupPushNotifications()
    }

    /**
     * Configurações iniciais de tema
     */
    private fun setupThemeSettings() {
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val themeMode = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        // Glass é uma skin overlay, não um modo noite — sempre aplicamos o tema base.
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    /**
     * Configurações iniciais de acessibilidade
     */
    private fun setupAccessibilitySettings() {
        // Verifica se alto contraste está ativo
        // Nota: Não aplicamos tema aqui para evitar conflitos com ThemeUtils
        // O tema deve ser aplicado apenas nas Activities através do ThemeUtils
        if (AccessibilityUtils.isHighContrastEnabled(this)) {
            // Apenas registra que alto contraste está ativo
            // O tema será aplicado nas Activities quando necessário
        }
    }

    /**
     * Agendamento diário do UpdateWorker (atualização de SITUACAO/VENCIMENTO).
     * Nome único "UpdateWorker" com política KEEP — evita jobs duplicados.
     * (Antes duplicado em MainActivity, MainActivity2 e App.kt — Fase 6)
     */
    private fun setupDailyWork() {
        val workRequest = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "UpdateWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Calcula o atraso até a próxima meia-noite (00:00)
     */
    private fun calculateInitialDelay(): Long {
        val now = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1) // Próximo dia
        }
        return targetTime.timeInMillis - now.timeInMillis
    }

    /**
     * Inicialização do OneSignal (notificações push)
     * (Antes no App.kt, que não era o Application registrado — as notificações não inicializavam)
     */
    private fun setupPushNotifications() {
        OneSignal.initWithContext(this, "8b6475b5-08dd-4991-83ec-caca9cc6be9e")
    }

    /**
     * Retorna o contexto da aplicação
     */
    fun getAppContext(): Context = applicationContext
}
