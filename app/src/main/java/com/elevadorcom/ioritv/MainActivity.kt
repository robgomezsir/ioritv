package com.elevadorcom.ioritv

import UpdateWorker
import android.content.Intent
import android.net.ParseException
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.elevadorcom.ioritv.databinding.ActivityMainBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import com.elevadorcom.ioritv.utils.SituacaoUtil
import com.elevadorcom.ioritv.utils.SituacaoConstants
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var editTextCreditos: EditText
    private lateinit var buttonSalvar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar o tema apropriado com base no modo da noite
        val isDarkMode = true // Troque para uma lógica adequada para definir o modo noturno
        if (isDarkMode) {
            setTheme(R.style.Theme_IORITv_MainActivity2_Dark)
        } else {
            setTheme(R.style.Base_Theme_IORITv_Dark)
        }

        super.onCreate(savedInstanceState)

        // Inflate o layout usando View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar a Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Adicionar Cadastro"

        // Inicializar o Firestore
        firestore = FirebaseFirestore.getInstance()

        // Inicializar o editTextCreditos com View Binding
        editTextCreditos = binding.editTextCreditos

        // Carregar clientes do Firebase Firestore
        carregarClientes()

        // Configurar restrições para os campos de VALOR, CUSTO e DESCONTO
        setupInputRestrictions()

        // Verificar se a Activity foi iniciada para edição
        verificarIntent()

        // Definir ação do botão "Salvar"
        binding.buttonSalvar.setOnClickListener {
            val intent = Intent(this, RankingActivity::class.java)
            // Passa os créditos inseridos pelo usuário para a RankingActivity
            intent.putExtra("creditosASubtrair", editTextCreditos.text.toString().toIntOrNull() ?: 0)
            startActivity(intent)

            // Salvar cliente no Firebase
            salvarCliente()
        }

        // Configura o WorkManager para executar a tarefa de atualização diariamente
        configurarWorkManager()

        // Executa o UpdateWorker ao abrir o aplicativo
        executarUpdateWorkerAoAbrir()
    }

    private fun verificarIntent() {
        val intent = intent
        if (intent.hasExtra("NOME")) {
            // Recupera os dados passados via Intent
            binding.editTextNome.setText(intent.getStringExtra("NOME"))
            binding.editTextUsuario.setText(intent.getStringExtra("USUARIO"))
            binding.editTextSenha.setText(intent.getStringExtra("SENHA"))
            binding.editTextWhatsapp.setText(intent.getStringExtra("WHATSAPP"))
            binding.editTextModelo.setText(intent.getStringExtra("MODELO"))

            val inicio = intent.getSerializableExtra("INICIO") as? Date
            binding.editTextInicio.setText(inicio?.formatToDate() ?: "")

            binding.editTextCreditos.setText(intent.getLongExtra("CREDITOS", 0).toString())
            binding.editTextMAC.setText(intent.getStringExtra("MAC"))
            binding.editTextOTP.setText(intent.getStringExtra("OTP"))
            binding.editTextDevice.setText(intent.getStringExtra("DEVICE"))
            binding.editTextValor.setText(intent.getDoubleExtra("VALOR", 0.0).toString())
            binding.editTextDesconto.setText(intent.getDoubleExtra("DESCONTO", 0.0).toString())
            binding.editTextCusto.setText(intent.getDoubleExtra("CUSTO", 0.0).toString())
            binding.editTextServidor.setText(intent.getStringExtra("SERVIDOR"))
        }
    }

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
        val valor = binding.editTextValor.text.toString().toDoubleOrNull() ?: 0.0
        val custo = binding.editTextCusto.text.toString().toDoubleOrNull() ?: 0.0
        val desconto = binding.editTextDesconto.text.toString().toDoubleOrNull() ?: 0.0
        val servidor = binding.editTextServidor.text.toString()

        // Conversão da data de INICIO para Timestamp
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val inicioDate = try {
            if (inicio.isNotEmpty()) {
                dateFormat.parse(inicio)
            } else {
                null
            }
        } catch (e: ParseException) {
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
                Toast.makeText(this, "Cliente salvo com sucesso!", Toast.LENGTH_SHORT).show()
                limparCampos()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao salvar cliente: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun limparCampos() {
        binding.editTextNome.text?.clear()
        binding.editTextUsuario.text?.clear()
        binding.editTextSenha.text?.clear()
        binding.editTextWhatsapp.text?.clear()
        binding.editTextModelo.text?.clear()
        binding.editTextInicio.text?.clear()
        binding.editTextCreditos.text?.clear()
        binding.editTextMAC.text?.clear()
        binding.editTextOTP.text?.clear()
        binding.editTextDevice.text?.clear()
        binding.editTextValor.text?.clear()
        binding.editTextCusto.text?.clear()
        binding.editTextDesconto.text?.clear()
        binding.editTextServidor.text?.clear()
    }

    // Função que ajusta as datas para ignorar horas, minutos e segundos
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
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt() // Converte milissegundos em dias
    }

    private fun Date.formatToDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(this)
    }

    private fun calculateTerminoDate(inicioDate: Date, creditos: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = inicioDate
        calendar.add(Calendar.MONTH, creditos) // Adiciona o número de meses de crédito à data de início
        return calendar.time
    }

    private fun setupInputRestrictions() {
        // Função para aplicar o TextWatcher nos campos de EditText
        val inputWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val cleanString = it.toString().filter { char ->
                        char.isDigit() || char == '.'
                    }

                    // Se a string foi alterada (continha caracteres inválidos), atualize o texto
                    if (cleanString != it.toString()) {
                        it.replace(0, it.length, cleanString)
                    }
                }
            }
        }

        // Adicionar o TextWatcher aos campos de VALOR, CUSTO e DESCONTO
        binding.editTextValor.addTextChangedListener(inputWatcher)
        binding.editTextCusto.addTextChangedListener(inputWatcher)
        binding.editTextDesconto.addTextChangedListener(inputWatcher)
    }

    private fun configurarWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val initialDelaySeconds = getInitialDelay()

        // Define o trabalho periódico, que você ainda pode manter
        val dailyWorkRequest = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS) // Define o atraso inicial
            .build()

        // Enfileira o trabalho periódico
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "UpdateWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyWorkRequest
        )

        // Força a execução imediata do Worker para testes
        WorkManager.getInstance(applicationContext).enqueue(
            OneTimeWorkRequestBuilder<UpdateWorker>().build()
        )
    }


    private fun carregarClientes() {
        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    // Processar os dados
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar clientes: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun executarUpdateWorkerAoAbrir() {
        // Define as restrições para o trabalho (opcional)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Garante que haja conexão de rede
            .build()

        // Cria a OneTimeWorkRequest para executar imediatamente
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<UpdateWorker>()
            .setConstraints(constraints)
            .build()

        // Enfileira o trabalho único, substituindo qualquer existente
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "UpdateWorkerOnAppOpen",
            ExistingWorkPolicy.REPLACE, // Substitui qualquer trabalho existente com o mesmo nome
            oneTimeWorkRequest
        )

        Log.d("MainActivity", "Worker enfileirado ao abrir o app")
    }


    private fun getInitialDelay(): Long {
        val calendar = Calendar.getInstance()

        // Define o horário atual
        val now = calendar.timeInMillis

        // Configura o calendário para a próxima meia-noite
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Adiciona um dia para apontar para a próxima meia-noite
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        val midnight = calendar.timeInMillis

        // Calcula a diferença em milissegundos e converte para segundos
        return (midnight - now) / 1000
    }
}
