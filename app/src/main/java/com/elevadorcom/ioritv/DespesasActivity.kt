package com.elevadorcom.ioritv

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.elevadorcom.ioritv.utils.ThemeUtils
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class DespesasActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var editTextData: EditText
    private lateinit var editTextDespesa: EditText
    private lateinit var editTextValor: EditText
    private lateinit var buttonSalvar: Button
    private var despesaId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica o tema apropriado usando ThemeUtils
        ThemeUtils.applyTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_despesas)

        firestore = FirebaseFirestore.getInstance()
        editTextData = findViewById(R.id.editTextData)
        editTextDespesa = findViewById(R.id.editTextDespesa)
        editTextValor = findViewById(R.id.editTextValor)
        buttonSalvar = findViewById(R.id.buttonSalvar)

        // Configurar TextWatcher para o campo de valor
        editTextValor.addTextChangedListener(object : TextWatcher {
            private var isEditing = false

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (isEditing) return

                isEditing = true

                val cleanString = s.toString().replace("[R$,.\\s]".toRegex(), "")
                val parsed = cleanString.toDoubleOrNull() ?: 0.0
                val formatted = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(parsed / 100)

                editTextValor.setText(formatted)
                editTextValor.setSelection(formatted.length)

                isEditing = false
            }
        })

        // Verificar se estamos editando uma despesa existente
        val intent = intent
        despesaId = intent.getStringExtra("DESPESA_ID")

        if (despesaId != null) {
            // Estamos editando uma despesa existente, buscar dados do Firestore
            fetchExpenseData(despesaId!!)
        }

        buttonSalvar.setOnClickListener {
            saveExpense()
        }
    }

    private fun fetchExpenseData(id: String) {
        firestore.collection("despesas").document(id).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val dataTimestamp = document.getTimestamp("Data")
                    val dataString = dataTimestamp?.toDate()?.let {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                    } ?: ""

                    val despesa = document.getString("Despesa") ?: ""
                    val valor = document.getDouble("Valor") ?: 0.0

                    editTextData.setText(dataString)
                    editTextDespesa.setText(despesa)
                    editTextValor.setText(NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor))
                } else {
                    Toast.makeText(this, "Despesa não encontrada", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar despesa: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveExpense() {
        val dataString = editTextData.text.toString()
        val despesa = editTextDespesa.text.toString()
        val valorString = editTextValor.text.toString().replace("[R$,.\\s]".toRegex(), "")
        val valor = valorString.toDoubleOrNull()?.div(100)

        // Verifica se todos os campos foram preenchidos
        if (dataString.isEmpty() || despesa.isEmpty() || valor == null) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Converte a data digitada em Timestamp
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dataParsed = formatter.parse(dataString)
        val dataTimestamp = dataParsed?.let { Timestamp(it) }

        if (dataTimestamp == null) {
            Toast.makeText(this, "Formato de data inválido", Toast.LENGTH_SHORT).show()
            return
        }

        val expense = hashMapOf(
            "Data" to dataTimestamp,  // Salva a data convertida
            "Despesa" to despesa,
            "Valor" to valor
        )

        if (despesaId != null) {
            firestore.collection("despesas").document(despesaId!!).set(expense)
                .addOnSuccessListener {
                    Toast.makeText(this, "Despesa atualizada com sucesso", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao atualizar despesa: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            firestore.collection("despesas").add(expense)
                .addOnSuccessListener {
                    Toast.makeText(this, "Despesa salva com sucesso", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao salvar despesa: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
