package com.elevadorcom.ioritv.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.NumberFormat
import java.util.Locale

/**
 * TextWatcher para formatação automática de valores monetários em reais (R$)
 * À medida que o usuário digita, o valor é formatado automaticamente
 */
class MoneyTextWatcher(
    private val editText: EditText,
    private val locale: Locale = Locale("pt", "BR")
) : TextWatcher {

    private var isEditing = false
    private val currencyFormat = NumberFormat.getCurrencyInstance(locale)

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // Não precisa fazer nada antes da mudança
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // Não precisa fazer nada durante a mudança
    }

    override fun afterTextChanged(s: Editable?) {
        if (isEditing) return

        isEditing = true

        try {
            // Remove todos os caracteres não numéricos
            val cleanString = s.toString().replace("[R$,.\\s]".toRegex(), "")
            
            // Converte para double (dividindo por 100 para considerar os centavos)
            val parsed = cleanString.toDoubleOrNull() ?: 0.0
            val formatted = currencyFormat.format(parsed / 100)

            // Atualiza o texto formatado
            editText.setText(formatted)
            editText.setSelection(formatted.length)
        } catch (e: Exception) {
            // Em caso de erro, mantém o texto original
            e.printStackTrace()
        } finally {
            isEditing = false
        }
    }

    companion object {
        /**
         * Extrai o valor numérico de um texto formatado como moeda
         * @param text Texto formatado (ex: "R$ 1.234,56")
         * @return Valor numérico (ex: 1234.56)
         */
        fun getNumericValue(text: String): Double {
            val cleanString = text.replace("[R$,.\\s]".toRegex(), "")
            return (cleanString.toDoubleOrNull() ?: 0.0) / 100
        }

        /**
         * Formata um valor numérico como moeda brasileira
         * @param value Valor numérico
         * @return Texto formatado (ex: "R$ 1.234,56")
         */
        fun formatCurrency(value: Double, locale: Locale = Locale("pt", "BR")): String {
            return NumberFormat.getCurrencyInstance(locale).format(value)
        }

        /**
         * Aplica o MoneyTextWatcher a um EditText
         * @param editText Campo de texto para aplicar a formatação
         */
        fun apply(editText: EditText) {
            editText.addTextChangedListener(MoneyTextWatcher(editText))
        }
    }
}

