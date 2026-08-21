package com.elevadorcom.ioritv

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.elevadorcom.ioritv.databinding.FragmentClienteFormBinding
import com.elevadorcom.ioritv.utils.MoneyTextWatcher
import com.elevadorcom.ioritv.utils.SituacaoUtil
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Formulário unificado de cliente (Fase 5) — substitui MainActivity (criar)
 * e EditCadastroActivity (editar). O modo é definido pelo argumento `clienteId`:
 * nulo/ausente = criar; preenchido = editar (carrega os dados do documento).
 */
class ClienteFormFragment : Fragment() {

    private var _binding: FragmentClienteFormBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private var cadastroId: String? = null
    private var initialDate: String? = null // Data inicial para comparação (só no modo edição)

    private val sharedPreferences by lazy {
        requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClienteFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Skin Glass: aplicar efeitos glassmorphism nos cards
        if (com.elevadorcom.ioritv.utils.ThemeUtils.isGlassEnabled(requireContext())) {
            com.elevadorcom.ioritv.utils.GlassUtils.applyGlassToFragment(view)
        }

        cadastroId = arguments?.getString("clienteId")

        setupInputRestrictions()

        // DatePicker para o campo Data Início
        binding.editTextInicio.isFocusable = false
        binding.editTextInicio.isClickable = true
        binding.editTextInicio.setOnClickListener {
            showMaterialDatePicker()
        }

        // Modo edição: carrega os dados do cliente
        cadastroId?.let { loadCadastroData(it) }

        binding.buttonSalvar.setOnClickListener {
            if (cadastroId == null) {
                salvarCliente()
            } else {
                updateCadastroData()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showMaterialDatePicker() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

        // Tenta parsear a data existente para selecionar no picker
        val currentDate = binding.editTextInicio.text.toString()
        val initialSelection = if (currentDate.isNotEmpty()) {
            try {
                sdf.parse(currentDate)?.time ?: MaterialDatePicker.todayInUtcMilliseconds()
            } catch (_: Exception) {
                MaterialDatePicker.todayInUtcMilliseconds()
            }
        } else {
            MaterialDatePicker.todayInUtcMilliseconds()
        }

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecione a data de início")
            .setSelection(initialSelection)
            .setTheme(R.style.Theme_IORITv_MaterialDatePicker)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            // MaterialDatePicker retorna UTC midnight — usar timezone UTC para evitar deslocamento de -1 dia
            val utc = java.util.TimeZone.getTimeZone("UTC")
            val calendar = Calendar.getInstance(utc).apply { timeInMillis = selection }
            val formatted = String.format(
                Locale("pt", "BR"),
                "%02d/%02d/%04d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR)
            )
            binding.editTextInicio.setText(formatted)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun loadCadastroData(cadastroId: String) {
        db.collection("clientes").document(cadastroId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Preenche os campos de texto
                    binding.editTextNome.setText(document.getString("NOME"))
                    binding.editTextUsuario.setText(document.getString("USUARIO"))
                    binding.editTextSenha.setText(document.getString("SENHA"))
                    binding.editTextWhatsapp.setText(document.getString("WHATSAPP"))
                    binding.editTextModelo.setText(document.getString("MODELO"))
                    binding.editTextMAC.setText(document.getString("MAC"))
                    binding.editTextOTP.setText(document.getString("OTP"))
                    binding.editTextDevice.setText(document.getString("DEVICE"))
                    binding.editTextServidor.setText(document.getString("SERVIDOR"))

                    // Campos financeiros formatados
                    binding.editTextValor.setText(MoneyTextWatcher.formatCurrency(document.getDouble("VALOR") ?: 0.0))
                    binding.editTextCusto.setText(MoneyTextWatcher.formatCurrency(document.getDouble("CUSTO") ?: 0.0))
                    binding.editTextDesconto.setText(MoneyTextWatcher.formatCurrency(document.getDouble("DESCONTO") ?: 0.0))

                    val inicioDate = document.getTimestamp("INICIO")?.toDate()
                    binding.editTextInicio.setText(inicioDate?.formatToDate() ?: "")
                    binding.editTextCreditos.setText(document.getLong("CREDITOS")?.toString() ?: "")

                    // Captura a data inicial APÓS o carregamento assíncrono
                    // (correção: na EditCadastroActivity original era capturada antes,
                    // o que fazia o ajuste de créditos disparar em toda edição)
                    initialDate = binding.editTextInicio.text.toString()
                } else {
                    Toast.makeText(requireContext(), "Cliente não encontrado", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao carregar os dados: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ----- CRIAR (antiga MainActivity) -----

    private fun salvarCliente() {
        val nome = binding.editTextNome.text.toString()
        val usuario = binding.editTextUsuario.text.toString()
        val senha = binding.editTextSenha.text.toString()
        val whatsapp = binding.editTextWhatsapp.text.toString()
        val modelo = binding.editTextModelo.text.toString()
        val inicio = binding.editTextInicio.text.toString()
        val creditos = binding.editTextCreditos.text.toString().toIntOrNull() ?: 0
        val mac = binding.editTextMAC.text.toString()
        val otp = binding.editTextOTP.text.toString()
        val device = binding.editTextDevice.text.toString()
        val valor = MoneyTextWatcher.getNumericValue(binding.editTextValor.text.toString())
        val custo = MoneyTextWatcher.getNumericValue(binding.editTextCusto.text.toString())
        val desconto = MoneyTextWatcher.getNumericValue(binding.editTextDesconto.text.toString())
        val servidor = binding.editTextServidor.text.toString()

        // Conversão da data de INICIO para Timestamp
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val inicioDate = try {
            if (inicio.isNotEmpty()) {
                dateFormat.parse(inicio)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        val inicioTimestamp = inicioDate?.let { Timestamp(it.time / 1000, 0) } // Converte para Timestamp

        // Calcular a data de TERMINO baseada no número de CREDITOS
        val terminoDate = if (inicioDate != null) {
            calculateTerminoDate(inicioDate, creditos)
        } else {
            Date()
        }
        val terminoTimestamp = Timestamp(terminoDate.time / 1000, 0) // Converte para Timestamp

        // Determinar a situação apenas para o cliente que está sendo salvo
        val situacao = SituacaoUtil.calcularSituacao(terminoDate)

        // Criação do mapa de cliente
        val cliente = hashMapOf(
            "NOME" to nome,
            "USUARIO" to usuario,
            "SENHA" to senha,
            "WHATSAPP" to whatsapp,
            "MODELO" to modelo,
            "INICIO" to inicioTimestamp,
            "CREDITOS" to creditos,
            "MAC" to mac,
            "OTP" to otp,
            "DEVICE" to device,
            "VALOR" to valor,
            "CUSTO" to custo,
            "DESCONTO" to desconto,
            "SERVIDOR" to servidor,
            "TERMINO" to terminoTimestamp,
            "SITUACAO" to situacao,
            "VENCIMENTO" to calcularVencimento(terminoDate)
        )

        // Salvar no Firebase Firestore
        db.collection("clientes")
            .add(cliente)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Cliente salvo com sucesso!", Toast.LENGTH_SHORT).show()
                // Volta para a lista de cadastros (o listener em tempo real atualiza)
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao salvar cliente: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ----- EDITAR (antiga EditCadastroActivity) -----

    private fun updateCadastroData() {
        val inicioDate = binding.editTextInicio.text.toString().toDate()
        val creditos = binding.editTextCreditos.text.toString().toIntOrNull() ?: 0

        if (inicioDate != null) {
            // Verifica se a data inicial foi alterada
            if (initialDate != binding.editTextInicio.text.toString()) {
                adjustTotalCredits(creditos) // Ajusta os créditos totais
            }

            val terminoDate = calculateTerminoDate(inicioDate, creditos)
            val situacao = SituacaoUtil.calcularSituacao(terminoDate)
            val vencimento = calcularVencimento(terminoDate)

            val valor = MoneyTextWatcher.getNumericValue(binding.editTextValor.text.toString())
            val custo = MoneyTextWatcher.getNumericValue(binding.editTextCusto.text.toString())
            val desconto = MoneyTextWatcher.getNumericValue(binding.editTextDesconto.text.toString())

            val updatedData = hashMapOf(
                "NOME" to binding.editTextNome.text.toString(),
                "USUARIO" to binding.editTextUsuario.text.toString(),
                "SENHA" to binding.editTextSenha.text.toString(),
                "WHATSAPP" to binding.editTextWhatsapp.text.toString(),
                "MODELO" to binding.editTextModelo.text.toString(),
                "MAC" to binding.editTextMAC.text.toString(),
                "OTP" to binding.editTextOTP.text.toString(),
                "DEVICE" to binding.editTextDevice.text.toString(),
                "SERVIDOR" to binding.editTextServidor.text.toString(),
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
                db.collection("clientes").document(it).update(updatedData as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Cadastro atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Erro ao atualizar cadastro: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        } else {
            // Exibe mensagem de erro se a data de início for inválida
            Toast.makeText(requireContext(), "Data de início inválida", Toast.LENGTH_LONG).show()
        }
    }

    private fun adjustTotalCredits(creditos: Int) {
        val currentTotal = sharedPreferences.getInt("totalCredit", 150)
        val newTotal = currentTotal - creditos
        sharedPreferences.edit().apply {
            putInt("totalCredit", newTotal)
            apply()
        }
        // A HomeFragment lê esta preferência ao voltar para atualizar sua UI
    }

    // ----- Helpers (idênticos aos originais) -----

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
        return calendar.time
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

    private fun Date.formatToDate(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(this) // Formata a data para string
    }

    private fun setupInputRestrictions() {
        // Aplicar formatação monetária automática aos campos de VALOR, CUSTO e DESCONTO
        MoneyTextWatcher.apply(binding.editTextValor)
        MoneyTextWatcher.apply(binding.editTextCusto)
        MoneyTextWatcher.apply(binding.editTextDesconto)
    }
}
