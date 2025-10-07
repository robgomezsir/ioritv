package com.elevadorcom.ioritv.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

/**
 * Utilitário para validar e verificar dependências críticas
 */
object DependencyValidator {

    private const val TAG = "DependencyValidator"

    /**
     * Valida se todas as dependências críticas estão funcionando
     */
    suspend fun validateDependencies(context: Context): ValidationResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<ValidationItem>()
        
        // Validar Firebase
        results.add(validateFirebase())
        
        // Validar Material Design
        results.add(validateMaterialDesign())
        
        // Validar Shimmer
        results.add(validateShimmer())
        
        // Validar Coroutines
        results.add(validateCoroutines())
        
        // Validar MPAndroidChart
        results.add(validateMPAndroidChart())
        
        val allValid = results.all { it.isValid }
        val criticalErrors = results.filter { !it.isValid && it.isCritical }
        
        Log.i(TAG, "Dependency validation completed. Valid: $allValid, Critical errors: ${criticalErrors.size}")
        
        ValidationResult(
            isValid = allValid,
            hasCriticalErrors = criticalErrors.isNotEmpty(),
            items = results
        )
    }

    private suspend fun validateFirebase(): ValidationItem = withContext(Dispatchers.IO) {
        return@withContext try {
            // Tentar acessar classes do Firebase
            Class.forName("com.google.firebase.firestore.FirebaseFirestore")
            Class.forName("com.google.firebase.auth.FirebaseAuth")
            ValidationItem(
                name = "Firebase",
                isValid = true,
                isCritical = true,
                message = "Firebase dependencies loaded successfully"
            )
        } catch (e: ClassNotFoundException) {
            ValidationItem(
                name = "Firebase",
                isValid = false,
                isCritical = true,
                message = "Firebase dependencies not found: ${e.message}"
            )
        }
    }

    private suspend fun validateMaterialDesign(): ValidationItem = withContext(Dispatchers.IO) {
        return@withContext try {
            Class.forName("com.google.android.material.card.MaterialCardView")
            Class.forName("com.google.android.material.button.MaterialButton")
            ValidationItem(
                name = "Material Design",
                isValid = true,
                isCritical = false,
                message = "Material Design components available"
            )
        } catch (e: ClassNotFoundException) {
            ValidationItem(
                name = "Material Design",
                isValid = false,
                isCritical = false,
                message = "Material Design components not found: ${e.message}"
            )
        }
    }

    private suspend fun validateShimmer(): ValidationItem = withContext(Dispatchers.IO) {
        return@withContext try {
            Class.forName("com.facebook.shimmer.ShimmerFrameLayout")
            ValidationItem(
                name = "Shimmer",
                isValid = true,
                isCritical = false,
                message = "Shimmer loading effects available"
            )
        } catch (e: ClassNotFoundException) {
            ValidationItem(
                name = "Shimmer",
                isValid = false,
                isCritical = false,
                message = "Shimmer library not found: ${e.message}"
            )
        }
    }

    private suspend fun validateCoroutines(): ValidationItem = withContext(Dispatchers.IO) {
        return@withContext try {
            Class.forName("kotlinx.coroutines.CoroutineScope")
            Class.forName("kotlinx.coroutines.Dispatchers")
            ValidationItem(
                name = "Kotlin Coroutines",
                isValid = true,
                isCritical = true,
                message = "Kotlin Coroutines available"
            )
        } catch (e: ClassNotFoundException) {
            ValidationItem(
                name = "Kotlin Coroutines",
                isValid = false,
                isCritical = true,
                message = "Kotlin Coroutines not found: ${e.message}"
            )
        }
    }

    private suspend fun validateMPAndroidChart(): ValidationItem = withContext(Dispatchers.IO) {
        return@withContext try {
            Class.forName("com.github.mikephil.charting.charts.BarChart")
            ValidationItem(
                name = "MPAndroidChart",
                isValid = true,
                isCritical = false,
                message = "MPAndroidChart library available"
            )
        } catch (e: ClassNotFoundException) {
            ValidationItem(
                name = "MPAndroidChart",
                isValid = false,
                isCritical = false,
                message = "MPAndroidChart library not found: ${e.message}"
            )
        }
    }

    /**
     * Resultado da validação de dependências
     */
    data class ValidationResult(
        val isValid: Boolean,
        val hasCriticalErrors: Boolean,
        val items: List<ValidationItem>
    )

    /**
     * Item individual de validação
     */
    data class ValidationItem(
        val name: String,
        val isValid: Boolean,
        val isCritical: Boolean,
        val message: String
    )
}
