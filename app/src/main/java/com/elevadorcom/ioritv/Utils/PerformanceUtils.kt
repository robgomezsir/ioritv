package com.elevadorcom.ioritv.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

object PerformanceUtils {

    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Executa uma tarefa em background
     */
    fun executeInBackground(task: suspend () -> Unit) {
        backgroundScope.launch {
            try {
                task()
            } catch (e: Exception) {
                // Log error if needed
            }
        }
    }

    /**
     * Otimiza um RecyclerView para melhor performance
     */
    fun optimizeRecyclerView(recyclerView: RecyclerView) {
        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(20)
        recyclerView.setDrawingCacheEnabled(true)
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH)
    }

    /**
     * Configura lazy loading para RecyclerView
     */
    fun setupLazyLoading(
        recyclerView: RecyclerView,
        onLoadMore: () -> Unit,
        threshold: Int = 5
    ) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager ?: return
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = when (layoutManager) {
                    is androidx.recyclerview.widget.LinearLayoutManager -> {
                        layoutManager.findLastVisibleItemPosition()
                    }
                    is androidx.recyclerview.widget.GridLayoutManager -> {
                        layoutManager.findLastVisibleItemPosition()
                    }
                    else -> return
                }
                
                if (lastVisibleItemPosition >= totalItemCount - threshold) {
                    onLoadMore()
                }
            }
        })
    }

    /**
     * Monitora uso de memória
     */
    fun getMemoryUsage(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        
        return MemoryInfo(
            totalMemory = totalMemory,
            usedMemory = usedMemory,
            freeMemory = freeMemory,
            maxMemory = maxMemory
        )
    }

    /**
     * Força garbage collection se necessário
     */
    fun forceGarbageCollectionIfNeeded() {
        val memoryInfo = getMemoryUsage()
        val memoryUsagePercent = (memoryInfo.usedMemory.toFloat() / memoryInfo.maxMemory.toFloat()) * 100
        
        if (memoryUsagePercent > 80) {
            System.gc()
        }
    }

    /**
     * Limpa cache de imagens se necessário
     */
    fun clearImageCacheIfNeeded() {
        val memoryInfo = getMemoryUsage()
        val memoryUsagePercent = (memoryInfo.usedMemory.toFloat() / memoryInfo.maxMemory.toFloat()) * 100
        
        if (memoryUsagePercent > 90) {
            // Implementar limpeza de cache de imagens se houver
            System.gc()
        }
    }

    /**
     * Configura monitoramento de performance
     */
    fun setupPerformanceMonitoring(context: Context) {
        // Monitorar memória periodicamente
        executeInBackground {
            while (true) {
                val memoryInfo = getMemoryUsage()
                if (memoryInfo.isMemoryLow()) {
                    forceGarbageCollectionIfNeeded()
                }
                if (memoryInfo.isMemoryCritical()) {
                    clearImageCacheIfNeeded()
                }
                delay(30000) // Verificar a cada 30 segundos
            }
        }
    }

    /**
     * Limpa recursos
     */
    fun cleanup() {
        backgroundScope.cancel()
    }

    /**
     * Data class para informações de memória
     */
    data class MemoryInfo(
        val totalMemory: Long,
        val usedMemory: Long,
        val freeMemory: Long,
        val maxMemory: Long
    ) {
        fun isMemoryLow(): Boolean {
            val usagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100
            return usagePercent > 75
        }
        
        fun isMemoryCritical(): Boolean {
            val usagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100
            return usagePercent > 90
        }
    }
}