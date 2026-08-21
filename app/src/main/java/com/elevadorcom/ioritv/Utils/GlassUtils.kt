package com.elevadorcom.ioritv.utils

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import com.elevadorcom.ioritv.R
import com.google.android.material.card.MaterialCardView

/**
 * Utilitário para aplicar efeitos glassmorphism programaticamente.
 *
 * Quando a skin Glass está ativada:
 * 1. Torna backgrounds de containers transparentes (FrameLayout, NestedScrollView, etc)
 * 2. Aplica bg_glass_card.xml como background de todos os MaterialCardView
 * 3. Remove bordas e elevation dos cards (glass cuida disso)
 */
object GlassUtils {

    /**
     * Aplica efeitos glass em toda a view hierarchy de uma fragment.
     * Chame no onViewCreated() de cada fragment quando glass estiver ativo.
     */
    fun applyGlassToFragment(rootView: View) {
        // 1. Tornar todos os containers transparentes
        makeTransparentRecursive(rootView)

        // 2. Aplicar glass cards
        applyGlassCardsRecursive(rootView)
    }

    /**
     * Torna backgrounds de containers conhecidos (FrameLayout, ScrollView, etc)
     * transparentes para o aurora brilhar através.
     */
    private fun makeTransparentRecursive(view: View) {
        when (view) {
            is FrameLayout -> {
                view.setBackgroundColor(Color.TRANSPARENT)
                view.setClipToPadding(false)
            }
            is NestedScrollView -> {
                view.setBackgroundColor(Color.TRANSPARENT)
                view.clipToPadding = false
                view.overScrollMode = View.OVER_SCROLL_NEVER
            }
            is ScrollView -> {
                view.setBackgroundColor(Color.TRANSPARENT)
                view.clipToPadding = false
            }
            is LinearLayout -> {
                // LinearLayouts internos dos cards não devem ser transparentes
                // Apenas LinearLayouts raiz de fragment
                if (view.parent is FrameLayout || view.parent is NestedScrollView) {
                    // Verificar se é o layout raiz do conteúdo
                    val bg = view.background
                    if (bg == null || (bg is android.graphics.drawable.ColorDrawable && bg.color == Color.TRANSPARENT)) {
                        // Já transparente, ok
                    }
                }
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                makeTransparentRecursive(view.getChildAt(i))
            }
        }
    }

    /**
     * Aplica bg_glass_card.xml como background de todos os MaterialCardView encontrados.
     * Remove cardBackgroundColor sólido e aplica o drawable glass com transparência.
     */
    private fun applyGlassCardsRecursive(view: View) {
        when (view) {
            is MaterialCardView -> {
                applyGlassCard(view)
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyGlassCardsRecursive(view.getChildAt(i))
            }
        }
    }

    /**
     * Aplica efeitos glass em um MaterialCardView específico.
     */
    private fun applyGlassCard(card: MaterialCardView) {
        val context = card.context

        // Background glass: superfície translúcida + sheen specular
        card.setBackgroundResource(R.drawable.bg_glass_card)

        // Borda luminosa glass
        card.strokeWidth = (1 * context.resources.displayMetrics.density).toInt() // 1dp
        card.strokeColor = context.getColor(R.color.glass_outline_variant)

        // Sem elevation (glass não usa sombra Material)
        card.cardElevation = 0f

        // Manter corner radius
        card.radius = context.resources.getDimension(R.dimen.shape_large)

        // Margem consistente
        val margin = (8 * context.resources.displayMetrics.density).toInt() // 8dp
        val lp = card.layoutParams as? ViewGroup.MarginLayoutParams
        lp?.setMargins(margin, margin, margin, margin)
    }

    /**
     * Aplica glass em chips e botões inline (ex: chips de filtro, botões de ação).
     */
    fun applyGlassToChip(view: View) {
        val context = view.context
        val gd = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(context.getColor(R.color.glass_surface))
            cornerRadius = context.resources.getDimension(R.dimen.shape_small)
            setStroke(
                (1 * context.resources.displayMetrics.density).toInt(),
                context.getColor(R.color.glass_outline_variant)
            )
        }
        view.background = gd
    }

    /**
     * Aplica glass em um botão outlined.
     */
    fun applyGlassToOutlinedButton(view: View) {
        view.setBackgroundResource(R.drawable.bg_glass_button_outlined)
    }
}
