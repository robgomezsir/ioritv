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
        val dialogBgColor = ContextCompat.getColor(context,
            if (isDark || isGlass) R.color.md_theme_dark_surfaceContainerHigh
            else R.color.md_theme_light_surfaceContainerHigh
        )
        val density = context.resources.displayMetrics.density

        // Background arredondado para o dialog
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(dialogBgColor)
            cornerRadius = 28 * density // extraLarge = 28dp
        }
        dialog.window?.setBackgroundDrawable(bgDrawable)

        dialog.setOnShowListener {
            // Cores baseadas no tema
            val primaryColor = ContextCompat.getColor(context,
                if (isDark || isGlass) R.color.md_theme_dark_primary
                else R.color.md_theme_light_primary
            )
            val onPrimaryColor = ContextCompat.getColor(context, R.color.white)

            // Botão positivo (Confirmar/Salvar) — fundo primary, texto white
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { button ->
                button.setTextColor(onPrimaryColor)
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
