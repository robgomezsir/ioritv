package com.elevadorcom.ioritv.utils

import android.content.Context
import android.content.res.Configuration
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton

object AccessibilityUtils {

    /**
     * Verifica se o TalkBack está ativo
     */
    fun isTalkBackEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        return accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled
    }

    /**
     * Verifica se o modo de alto contraste está ativo
     */
    fun isHighContrastEnabled(context: Context): Boolean {
        val configuration = context.resources.configuration
        return (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Aplica configurações de acessibilidade a um card
     */
    fun setupCardAccessibility(card: MaterialCardView, description: String, isClickable: Boolean = false) {
        card.contentDescription = description
        card.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        
        if (isClickable) {
            card.isClickable = true
            card.isFocusable = true
        }
    }

    /**
     * Configura um botão para acessibilidade
     */
    fun setupButtonAccessibility(button: MaterialButton, description: String) {
        button.contentDescription = description
        button.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        button.isFocusable = true
    }

    /**
     * Anuncia uma mudança para o usuário via TalkBack
     */
    fun announceForAccessibility(view: View, text: String) {
        view.announceForAccessibility(text)
    }

    /**
     * Configura navegação por teclado para uma view
     */
    fun setupKeyboardNavigation(view: View, nextFocusDown: View? = null, nextFocusUp: View? = null) {
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        
        nextFocusDown?.let { view.nextFocusDownId = it.id }
        nextFocusUp?.let { view.nextFocusUpId = it.id }
    }

    /**
     * Cria uma descrição acessível para gráficos
     */
    fun createChartDescription(title: String, data: List<Pair<String, Float>>): String {
        val description = StringBuilder("Gráfico: $title. ")
        data.forEach { (label, value) ->
            description.append("$label: $value. ")
        }
        return description.toString()
    }

    /**
     * Cria uma descrição acessível para progresso
     */
    fun createProgressDescription(current: Int, total: Int, label: String): String {
        val percentage = if (total > 0) (current * 100 / total) else 0
        return "$label: $current de $total ($percentage%)"
    }

    /**
     * Configura uma lista para navegação acessível
     */
    fun setupListAccessibility(parent: View, itemCount: Int, itemType: String) {
        parent.contentDescription = "$itemType com $itemCount itens"
        parent.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    /**
     * Configura um campo de entrada para acessibilidade
     */
    fun setupInputAccessibility(view: View, label: String, hint: String, error: String? = null) {
        view.contentDescription = if (error != null) "$label, $hint, Erro: $error" else "$label, $hint"
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    /**
     * Configura navegação sequencial para múltiplas views
     */
    fun setupSequentialNavigation(views: List<View>) {
        views.forEachIndexed { index, view ->
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            
            // Configura navegação para próxima view
            if (index < views.size - 1) {
                view.nextFocusDownId = views[index + 1].id
            }
            
            // Configura navegação para view anterior
            if (index > 0) {
                view.nextFocusUpId = views[index - 1].id
            }
        }
    }

    /**
     * Aplica tema de alto contraste se necessário
     */
    fun applyHighContrastTheme(context: Context, isHighContrast: Boolean) {
        val theme = if (isHighContrast) {
            com.elevadorcom.ioritv.R.style.Base_Theme_IORITv_HighContrast
        } else {
            com.elevadorcom.ioritv.R.style.Base_Theme_IORITv
        }
        // Não aplicar tema aqui pois pode causar conflitos com ThemeUtils
        // O tema deve ser aplicado apenas no ThemeUtils
    }

    /**
     * Configura feedback háptico para ações importantes
     */
    fun setupHapticFeedback(view: View) {
        view.isHapticFeedbackEnabled = true
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /**
     * Cria uma descrição acessível para status de dados
     */
    fun createDataStatusDescription(status: String, count: Int): String {
        return when (status.lowercase()) {
            "ativo" -> "$count clientes ativos"
            "inativo" -> "$count clientes inativos"
            "pendente" -> "$count clientes pendentes"
            "vencido" -> "$count clientes vencidos"
            else -> "$count itens com status $status"
        }
    }
}