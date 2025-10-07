package com.elevadorcom.ioritv

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.elevadorcom.ioritv.databinding.ActivityEditCadastroBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.elevadorcom.ioritv.utils.SituacaoUtil
import com.elevadorcom.ioritv.utils.ThemeUtils

class EditCadastroActivity : AppCompatActivity() {

    // Declaração de variáveis para o binding e Firestore
    private lateinit var binding: ActivityEditCadastroBinding
    private lateinit var firestore: FirebaseFirestore
    private var initialDate: String? = null // Data inicial para comparação
    private var cadastroId: String? = null // ID do cadastro
    private val sharedPreferences by lazy {
        // Inicializa SharedPreferences para armazenar dados persistentes
        getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica o tema apropriado usando ThemeUtils
        ThemeUtils.applyTheme(this)

        super.onCreate(savedInstanceState)
        binding = ActivityEditCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        cadastroId = intent.getStringExtra("cadastroId") // Obtém o ID do cadastro

        setupInputRestrictions() // Configura restrições de entrada

        cadastroId?.let {
            loadCadastroData(it) // Carrega os dados do cadastro se o ID não for nulo
        }

        binding.apply {
            initialDate = inicioEditText.text.toString() // Captura a data inicial
        }

        binding.saveButton.setOnClickListener {
            updateCadastroData() // Configura o listener do botão salvar
        }
    }

    private fun loadCadastroData(cadastroId: String) {
        val docRef = firestore.collection("clientes").document(cadastroId)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Carrega os dados nos campos de texto
                    binding.nomeEditText.setText(document.getString("NOME"))
                    binding.usuarioEditText.setText(document.getString("USUARIO"))
                    binding.senhaEditText.setText(document.getString("SENHA"))
                    binding.whatsappEditText.setText(document.getString("WHATSAPP"))
                    binding.modeloEditText.setText(document.getString("MODELO"))
                    binding.macEditText.setText(document.getString("MAC"))
                    binding.otpEditText.setText(document.getString("OTP"))
                    binding.deviceEditText.setText(document.getString("DEVICE"))
                    binding.servidorEditText.setText(document.getString("SERVIDOR"))

                    val valor = document.getDouble("VALOR") ?: 0.0
                    val custo = document.getDouble("CUSTO") ?: 0.0
                    val desconto = document.getDouble("DESCONTO") ?: 0.0

                    binding.valorEditText.setText(valor.toString())
                    binding.custoEditText.setText(custo.toString())
                    binding.descontoEditText.setText(desconto.toString())

                    val inicioDate = document.getTimestamp("INICIO")?.toDate()
                    binding.inicioEditText.setText(inicioDate?.toFormattedString() ?: "")
                    binding.creditosEditText.setText(document.getLong("CREDITOS")?.toString() ?: "")
                }
            }
            .addOnFailureListener { e ->
                // Exibe mensagem de erro se falhar ao carregar os dados
                Toast.makeText(this, "Erro ao carregar os dados: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateCadastroData() {
        val inicioDate = binding.inicioEditText.text.toString().toDate()
        val creditos = binding.creditosEditText.text.toString().toIntOrNull() ?: 0

        if (inicioDate != null) {
            // Verifica se a data inicial foi alterada
            if (initialDate != binding.inicioEditText.text.toString()) {
                adjustTotalCredits(creditos) // Ajusta os créditos totais
            }

            val terminoDate = calculateTerminoDate(inicioDate, creditos)
            val situacao = SituacaoUtil.calcularSituacao(terminoDate)
            val vencimento = calcularVencimento(terminoDate)

            val valor = binding.valorEditText.text.toString().toDoubleOrNull() ?: 0.0
            val custo = binding.custoEditText.text.toString().toDoubleOrNull() ?: 0.0
            val desconto = binding.descontoEditText.text.toString().toDoubleOrNull() ?: 0.0

            val updatedData = hashMapOf(
                "NOME" to binding.nomeEditText.text.toString(),
                "USUARIO" to binding.usuarioEditText.text.toString(),
                "SENHA" to binding.senhaEditText.text.toString(),
                "WHATSAPP" to binding.whatsappEditText.text.toString(),
                "MODELO" to binding.modeloEditText.text.toString(),
                "MAC" to binding.macEditText.text.toString(),
                "OTP" to binding.otpEditText.text.toString(),
                "DEVICE" to binding.deviceEditText.text.toString(),
                "SERVIDOR" to binding.servidorEditText.text.toString(),
                "INICIO" to inicioDate,
                "CREDITOS" to creditos,
                "VALOR" to valor,
                "CUSTO" to custo,
                "DESCONTO" to desconto,
                "TERMINO" to terminoDate,
                "SITUACAO" to situacao,
                "VENCIMENTO" to vencimento
            )

            cadastroId?.let {
                firestore.collection("clientes").document(it).update(updatedData as Map<String, Any>)
                    .addOnSuccessListener {
                        // Exibe mensagem de sucesso ao atualizar o cadastro
                        Toast.makeText(this, "Cadastro atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        // Exibe mensagem de erro se falhar ao atualizar o cadastro
                        Toast.makeText(this, "Erro ao atualizar cadastro: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        } else {
            // Exibe mensagem de erro se a data de início for inválida
            Toast.makeText(this, "Data de início inválida", Toast.LENGTH_LONG).show()
        }
    }

    private fun adjustTotalCredits(creditos: Int) {
        val sharedPref = sharedPreferences
        val currentTotal = sharedPref.getInt("totalCredit", 150)
        val newTotal = currentTotal - creditos
        with(sharedPref.edit()) {
            putInt("totalCredit", newTotal)
            apply()
        }
        // Assume que a RankingActivity lerá esta preferência para atualizar sua UI
    }

    private fun calculateTerminoDate(inicioDate: Date, creditos: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = inicioDate
        calendar.add(Calendar.MONTH, creditos) // Adiciona o número de meses de crédito à data de início
        return calendar.time
    }

    private fun clearTime(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time // Retorna a data com o tempo zerado
    }

    private fun calcularVencimento(termino: Date): String {
        val hoje = clearTime(Date()) // Limpa o horário da data de hoje
        val terminoSemHora = clearTime(termino) // Limpa o horário da data de término
        val diasRestantes = calculateDaysDifference(hoje, terminoSemHora)

        return when {
            diasRestantes > 2 -> "Faltam $diasRestantes dias"
            diasRestantes in 1..2 -> "Ainda falta(m) $diasRestantes dia(s)"
            diasRestantes == 0 -> "Vence hoje"
            diasRestantes < 0 -> "Já são ${-diasRestantes} dias vencidos"
            else -> "Faltam $diasRestantes dias"
        }
    }

    private fun calculateDaysDifference(hoje: Date, termino: Date): Int {
        val diffInMillis = termino.time - hoje.time
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt() // Calcula a diferença em dias
    }

    private fun String.toDate(): Date? {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(this) // Converte string para data
        } catch (e: Exception) {
            null
        }
    }

    private fun Date.toFormattedString(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(this) // Formata a data para string
    }

    private fun setupInputRestrictions() {
        val inputWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val cleanString = it.toString().filter { char ->
                        char.isDigit() || char == '.'
                    }

                    if (cleanString != it.toString()) {
                        it.replace(0, it.length, cleanString) // Remove caracteres inválidos
                    }
                }
            }
        }

        // Aplica o TextWatcher aos campos de entrada
        binding.valorEditText.addTextChangedListener(inputWatcher)
        binding.custoEditText.addTextChangedListener(inputWatcher)
        binding.descontoEditText.addTextChangedListener(inputWatcher)
    }
}