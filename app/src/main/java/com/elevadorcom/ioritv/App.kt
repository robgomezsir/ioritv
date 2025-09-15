package com.elevadorcom.ioritv

import UpdateWorker
import android.app.Application
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.onesignal.OneSignal

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        setupDailyWork()
        OneSignal.initWithContext(this, "8b6475b5-08dd-4991-83ec-caca9cc6be9e")
    }

    private fun setupDailyWork() {
        val workRequest = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "UpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun calculateInitialDelay(): Long {
        val now = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1) // Próximo dia
        }
        return targetTime.timeInMillis - now.timeInMillis
    }
}
