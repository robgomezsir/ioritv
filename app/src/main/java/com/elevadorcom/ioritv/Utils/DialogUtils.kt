package com.elevadorcom.ioritv.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.elevadorcom.ioritv.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Utilitário para estilizar AlertDialogs com Material3.
 * Fundo sólido, cantos arredondados, sem bordas, cores semânticas.
 */
object DialogUtils {

    /**
     * Cria um MaterialAlertDialog Builder com tema Material3.
     */
    fun materialDialog(context: Context): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(context)
    }

    /**
     * Aplica estilos Material3 aos botões e fundo do AlertDialog.
     */
    fun styleAlertDialogButtons(dialog: AlertDialog, context: Context) {
        val isDark = ThemeUtils.isDarkTheme(context)
        val isGlass = ThemeUtils.isGlassEnabled(context)

        // Cor de fundo do dialog
        // Glass: quase opaco (95% branco/preto) para legibilidade
        val dialogBgColor = when {
            isGlass && isDark -> 0xF20E1415.toInt()  // 95% opaco escuro
            isGlass && !isDark -> 0xF2FFFFFF.toInt()  // 95% opaco claro
            isDark -> ContextCompat.getColor(context, R.color.md_theme_dark_surfaceContainerHigh)
            else -> ContextCompat.getColor(context, R.color.md_theme_light_surfaceContainerHigh)
        }
        val density = context.resources.displayMetrics.density

        // Background arredondado para o dialog
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(dialogBgColor)
            cornerRadius = 28 * density // extraLarge = 28dp
        }
        dialog.window?.setBackgroundDrawable(bgDrawable)

        // Overlay scrim mais escuro no glass para contraste
        if (isGlass) {
            dialog.window?.setDimAmount(0.7f)
        }

        dialog.setOnShowListener {
            // Cores baseadas no tema
            val primaryColor = ContextCompat.getColor(context,
                if (isDark || isGlass) R.color.md_theme_dark_primary
                else R.color.md_theme_light_primary
            )
            val textColor = when {
                isGlass && isDark -> 0xFFDEE3E5.toInt()  // texto claro glass
                isGlass && !isDark -> 0xFF171D1E.toInt()  // texto escuro glass
                else -> ContextCompat.getColor(context, R.color.white)
            }

            // Botão positivo (Confirmar/Salvar) — fundo primary, texto branco
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { button ->
                button.setTextColor(textColor)
                button.background = createRoundedButton(context, primaryColor)
                button.minimumWidth = 0
                button.minimumHeight = 0
                val padding = (16 * density).toInt()
                button.setPadding(padding * 2, padding, padding * 2, padding)
            }

            // Botão negativo (Cancelar) — fundo transparente, texto primary
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { button ->
                button.setTextColor(primaryColor)
                button.background = createRoundedButton(context, Color.TRANSPARENT)
                button.minimumWidth = 0
                button.minimumHeight = 0
                val padding = (16 * density).toInt()
                button.setPadding(padding * 2, padding, padding * 2, padding)
            }

            // Botão neutro
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.let { button ->
                button.setTextColor(primaryColor)
                button.background = createRoundedButton(context, Color.TRANSPARENT)
                button.minimumWidth = 0
                button.minimumHeight = 0
                val padding = (16 * density).toInt()
                button.setPadding(padding * 2, padding, padding * 2, padding)
            }
        }
    }

    /**
     * Estilo para tema escuro — delega para styleAlertDialogButtons.
     */
    fun styleAlertDialogButtonsDark(dialog: AlertDialog, context: Context) {
        styleAlertDialogButtons(dialog, context)
    }

    /**
     * Cria fundo arredondado para botões de dialog.
     */
    private fun createRoundedButton(context: Context, bgColor: Int): GradientDrawable {
        val density = context.resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(bgColor)
            cornerRadius = 12 * density
        }
    }
}
