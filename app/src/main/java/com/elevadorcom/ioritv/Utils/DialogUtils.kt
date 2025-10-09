package com.elevadorcom.ioritv.utils

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

/**
 * Utilitário para estilizar AlertDialogs com contraste adequado
 */
object DialogUtils {
    
    /**
     * Aplica estilos aos botões do AlertDialog para garantir contraste adequado
     */
    fun styleAlertDialogButtons(dialog: AlertDialog, context: Context) {
        dialog.setOnShowListener {
            // Botão positivo (Salvar)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { button ->
                button.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                button.setBackgroundColor(ContextCompat.getColor(context, android.R.color.black))
                button.setPadding(32, 16, 32, 16)
            }
            
            // Botão negativo (Cancelar)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { button ->
                button.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                button.setBackgroundColor(Color.TRANSPARENT)
                button.setPadding(32, 16, 32, 16)
            }
        }
    }
    
    /**
     * Aplica estilos aos botões do AlertDialog para tema escuro
     */
    fun styleAlertDialogButtonsDark(dialog: AlertDialog, context: Context) {
        dialog.setOnShowListener {
            // Botão positivo (Salvar)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { button ->
                button.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                button.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                button.setPadding(32, 16, 32, 16)
            }
            
            // Botão negativo (Cancelar)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { button ->
                button.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                button.setBackgroundColor(Color.TRANSPARENT)
                button.setPadding(32, 16, 32, 16)
            }
        }
    }
}
