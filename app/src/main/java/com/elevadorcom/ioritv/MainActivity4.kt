package com.elevadorcom.ioritv

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.elevadorcom.ioritv.utils.AccessibilityUtils
import com.elevadorcom.ioritv.utils.AnimationUtils
// import com.elevadorcom.ioritv.utils.DialogUtils
import com.elevadorcom.ioritv.utils.DialogUtils
import com.elevadorcom.ioritv.utils.ThemeUtils
import com.elevadorcom.ioritv.utils.MoneyTextWatcher
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity4 : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var fabMenu: FloatingActionButton
    private lateinit var fabNovaDespesa: FloatingActionButton
    private lateinit var fabDownload: FloatingActionButton
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var recyclerViewDespesas: RecyclerView
    private lateinit var despesaAdapter: DespesaAdapter
    
    // Cards
    private lateinit var custoTotalCard: MaterialCardView
    
    // TextViews para métricas
    private lateinit var textTotalClientes: TextView
    private lateinit var textContasAtivas: TextView
    private lateinit var textContasVencidas: TextView
    private lateinit var textContasAVencer: TextView
    private lateinit var textContasInativas: TextView
    private lateinit var textClientesGerandoReceita: TextView
    private lateinit var textTotalVendas: TextView
    private lateinit var textCustoTotal: TextView
    private lateinit var textLucroLiquido: TextView
    private lateinit var textDespesas: TextView
    private lateinit var textLucroLiquidoFinal: TextView
    private lateinit var textMargemLucro: TextView
    
    private val db = FirebaseFirestore.getInstance()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private var custoTotalFixo = 0.0
    private var subFabOpen = false
    
    // Launcher para requisição de permissões
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            proceedWithDownload()
        } else {
            Toast.makeText(this, "Permissão de armazenamento necessária para salvar o arquivo", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica o tema apropriado usando ThemeUtils
        ThemeUtils.applyTheme(this)
        
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main4)

        initializeViews()
        setupToolbar()
        setupFloatingActionButtons()
        setupBottomNavigation()
        setupCustoTotalCard()
        setupDespesaRecyclerView()
        setupAccessibility()
        loadCustoTotalFromFirebase()
        loadFinancialMetrics()
        loadDespesasList()
        
        // DESENVOLVIMENTO: Descomente a linha abaixo para zerar todos os registros de despesas
        // clearAllDespesas()
    }
    
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
            fabMenu = findViewById(R.id.fabMenu)
            fabNovaDespesa = findViewById(R.id.fabNovaDespesa)
            fabDownload = findViewById(R.id.fabDownload)
            bottomNavigation = findViewById(R.id.bottom_navigation)
        recyclerViewDespesas = findViewById(R.id.recyclerViewDespesas)
            custoTotalCard = findViewById(R.id.custoTotalCard)
        
        textTotalClientes = findViewById(R.id.textTotalClientes)
        textContasAtivas = findViewById(R.id.textContasAtivas)
        textContasVencidas = findViewById(R.id.textContasVencidas)
        textContasAVencer = findViewById(R.id.textContasAVencer)
        textContasInativas = findViewById(R.id.textContasInativas)
        textClientesGerandoReceita = findViewById(R.id.textClientesGerandoReceita)
        textTotalVendas = findViewById(R.id.textTotalVendas)
        textCustoTotal = findViewById(R.id.textCustoTotal)
        textLucroLiquido = findViewById(R.id.textLucroLiquido)
        textDespesas = findViewById(R.id.textDespesas)
        textLucroLiquidoFinal = findViewById(R.id.textLucroLiquidoFinal)
        textMargemLucro = findViewById(R.id.textMargemLucro)
    }
    
    private fun setupCustoTotalCard() {
        custoTotalCard.setOnClickListener {
            AnimationUtils.animateButtonClick(it) {
                showEditCustoTotalDialog()
            }
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        val title = SpannableString("Meu Ioritv")
        title.setSpan(StyleSpan(Typeface.BOLD), 0, title.length, 0)
        supportActionBar?.title = title
    }
    
    private fun setupFloatingActionButtons() {
        // FAB principal - toggle dos sub-FABs
        fabMenu.setOnClickListener {
            toggleSubFabs()
        }
        
        // Sub-FAB Nova Despesa
        fabNovaDespesa.setOnClickListener {
            hideSubFabs()
            // Rotacionar FAB principal de volta ao estado original
            fabMenu.animate()
                .rotation(0f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
            subFabOpen = false
            showAddDespesaDialog()
        }
        
        // Sub-FAB Download
        fabDownload.setOnClickListener {
            hideSubFabs()
            // Rotacionar FAB principal de volta ao estado original
            fabMenu.animate()
                .rotation(0f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
            subFabOpen = false
            downloadClientesList()
        }
        
        // Configurar acessibilidade
        setupFABAccessibility()
    }
    
    private fun setupFABAccessibility() {
        fabMenu.contentDescription = "Menu de ações"
        fabMenu.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        
        fabNovaDespesa.contentDescription = "Adicionar nova despesa"
        fabNovaDespesa.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        
        fabDownload.contentDescription = "Download da lista de clientes"
        fabDownload.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, RankingActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_cadastros -> {
                    startActivity(Intent(this, MainActivity2::class.java))
                    finish()
                    true
                }
                R.id.nav_financas -> {
                    // Já estamos na tela de finanças
                    true
                }
                else -> false
            }
        }

        // Marcar o item de finanças como selecionado
        bottomNavigation.selectedItemId = R.id.nav_financas
    }
    
    private fun setupAccessibility() {
        // Configurar acessibilidade para os cards - Seção Cadastros
        AccessibilityUtils.setupCardAccessibility(
            findViewById(R.id.totalClientesCard),
            "Card de total de clientes cadastrados"
        )
        
        AccessibilityUtils.setupCardAccessibility(
            findViewById(R.id.contasAtivasCard),
            "Card de contas ativas, clientes com créditos ativos adimplentes"
        )
        
        AccessibilityUtils.setupCardAccessibility(
            findViewById(R.id.contasVencidasCard),
            "Card de contas vencidas, clientes com créditos vencidos a mais de 15 dias"
        )
        
        AccessibilityUtils.setupCardAccessibility(
            findViewById(R.id.contasAVencerCard),
            "Card de contas a vencer, clientes com créditos a 3 dias do vencimento"
        )
        
        AccessibilityUtils.setupCardAccessibility(
            findViewById(R.id.contasInativasCard),
            "Card de contas inativas, clientes com créditos vencidos a mais de 30 dias"
        )
        
        AccessibilityUtils.setupCardAccessibility(
            findViewById(R.id.clientesGerandoReceitaCard),
            "Card de clientes gerando receita, total de clientes ativos e a vencer"
        )
        
        // Configurar acessibilidade para os cards - Seção Finanças
        AccessibilityUtils.setupCardAccessibility(
            findViewById(R.id.totalVendasCard),
            "Card de total de vendas, soma de todos os valores de clientes ativos"
        )
        
        AccessibilityUtils.setupCardAccessibility(
            findViewById(R.id.custoTotalCard),
            "Card de custo total operacional, toque para editar"
        )
        
        AccessibilityUtils.setupCardAccessibility(
            findViewById(R.id.lucroLiquidoCard),
            "Card de lucro líquido total, calculado como total de vendas menos custo total"
        )
        
        AccessibilityUtils.setupCardAccessibility(
            findViewById(R.id.despesasCard),
            "Card de despesas operacionais"
        )
        
        AccessibilityUtils.setupCardAccessibility(
            findViewById(R.id.lucroLiquidoFinalCard),
            "Card de lucro líquido final, calculado como lucro líquido total menos despesas"
        )
        
        AccessibilityUtils.setupCardAccessibility(
            findViewById(R.id.margemLucroCard),
            "Card de margem de lucro, percentual de lucro sobre vendas"
        )
        
    }
    
    private fun loadCustoTotalFromFirebase() {
        db.collection("configuracoes")
            .document("custoTotal")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    custoTotalFixo = document.getDouble("valor") ?: 0.0
                } else {
                    custoTotalFixo = 0.0
                }
                textCustoTotal.text = currencyFormat.format(custoTotalFixo)
            }
            .addOnFailureListener {
                custoTotalFixo = 0.0
                textCustoTotal.text = currencyFormat.format(custoTotalFixo)
            }
    }
    
    private fun saveCustoTotalToFirebase(valor: Double) {
        val custoData = hashMapOf(
            "valor" to valor,
            "ultimaAtualizacao" to System.currentTimeMillis()
        )
        
        db.collection("configuracoes")
            .document("custoTotal")
            .set(custoData)
            .addOnSuccessListener {
                custoTotalFixo = valor
                textCustoTotal.text = currencyFormat.format(valor)
                Toast.makeText(this, "Custo total atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                // Recarregar métricas para atualizar lucro
                loadFinancialMetrics()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    private fun showEditCustoTotalDialog() {
        val input = EditText(this)
        input.hint = "Digite o custo total"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        
        // Aplicar formatação monetária automática
        MoneyTextWatcher.apply(input)
        
        // Pré-preencher com valor atual
        if (custoTotalFixo > 0) {
            input.setText(MoneyTextWatcher.formatCurrency(custoTotalFixo))
        }
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Definir Custo Total")
            .setMessage("Informe o custo operacional mensal fixo:")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val valor = MoneyTextWatcher.getNumericValue(input.text.toString())
                
                if (valor >= 0) {
                    saveCustoTotalToFirebase(valor)
                } else {
                    Toast.makeText(this, "Por favor, insira um valor válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        
        if (ThemeUtils.isDarkTheme(this)) {
            DialogUtils.styleAlertDialogButtonsDark(dialog, this)
        } else {
            DialogUtils.styleAlertDialogButtons(dialog, this)
        }
        dialog.show()
    }
    
    private fun loadFinancialMetrics() {
        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                var totalClientes = 0
                var contasVencidas = 0
                var contasAVencer = 0
                var contasAtivas = 0
                var contasInativas = 0
                var totalVendas = 0.0
                
                for (document in result) {
                    totalClientes++
                    val situacao = document.getString("SITUACAO") ?: ""
                    val terminoTimestamp = document.getTimestamp("TERMINO")
                    val valor = document.getDouble("VALOR") ?: 0.0
                    
                    // Calcular dias até vencimento
                    val diasParaVencimento = if (terminoTimestamp != null) {
                        val hoje = clearTime(Date())
                        val terminoDate = clearTime(terminoTimestamp.toDate())
                        calculateDaysDifference(hoje, terminoDate)
                    } else {
                        0
                    }
                    
                    // Classificar por situação e dias
                    when {
                        situacao == "STANDBY" || diasParaVencimento <= -30 -> contasInativas++
                        situacao == "VENCIDO" || (diasParaVencimento <= -15 && diasParaVencimento > -30) -> contasVencidas++
                        situacao == "A VENCER" || (diasParaVencimento >= 0 && diasParaVencimento <= 3) -> {
                            contasAVencer++
                            // Incluir no total de vendas
                            totalVendas += valor
                        }
                        situacao == "ATIVO" || diasParaVencimento > 3 -> {
                            contasAtivas++
                            // Incluir no total de vendas
                            totalVendas += valor
                        }
                    }
                }
                
                // Usar o custo total fixo
                val custoTotal = custoTotalFixo
                
                // Carregar despesas do Firebase
                loadDespesasFromFirebase { despesas ->
                    // Calcular lucro líquido total
                    val lucroLiquido = totalVendas - custoTotal
                    
                    // Calcular lucro líquido final
                    val lucroLiquidoFinal = lucroLiquido - despesas
                    
                    // Calcular margem de lucro
                    val margemLucro = if (totalVendas > 0) {
                        (lucroLiquidoFinal / totalVendas) * 100
                    } else {
                        0.0
                    }
                    
                    // Clientes gerando receita = ativos + a vencer
                    val clientesGerandoReceita = contasAtivas + contasAVencer
                    
                    // Atualizar UI
                    updateFinancialUI(
                        totalClientes,
                        contasAtivas,
                        contasVencidas,
                        contasAVencer,
                        contasInativas,
                        clientesGerandoReceita,
                        totalVendas,
                        custoTotal,
                        lucroLiquido,
                        despesas,
                        lucroLiquidoFinal,
                        margemLucro
                    )
                }
            }
            .addOnFailureListener { e ->
                // Em caso de erro, manter valores zerados
                updateFinancialUI(0, 0, 0, 0, 0, 0, 0.0, custoTotalFixo, -custoTotalFixo, 0.0, -custoTotalFixo, 0.0)
            }
    }
    
    private fun updateFinancialUI(
        totalClientes: Int,
        contasAtivas: Int,
        contasVencidas: Int,
        contasAVencer: Int,
        contasInativas: Int,
        clientesGerandoReceita: Int,
        totalVendas: Double,
        custoTotal: Double,
        lucroLiquido: Double,
        despesas: Double,
        lucroLiquidoFinal: Double,
        margemLucro: Double
    ) {
        // Seção Cadastros
        textTotalClientes.text = totalClientes.toString()
        textContasAtivas.text = contasAtivas.toString()
        textContasVencidas.text = contasVencidas.toString()
        textContasAVencer.text = contasAVencer.toString()
        textContasInativas.text = contasInativas.toString()
        textClientesGerandoReceita.text = clientesGerandoReceita.toString()
        
        // Seção Finanças
        textTotalVendas.text = currencyFormat.format(totalVendas)
        textCustoTotal.text = currencyFormat.format(custoTotal)
        textLucroLiquido.text = currencyFormat.format(lucroLiquido)
        textDespesas.text = currencyFormat.format(despesas)
        textLucroLiquidoFinal.text = currencyFormat.format(lucroLiquidoFinal)
        textMargemLucro.text = String.format(Locale.getDefault(), "%.2f%%", margemLucro)
        
        // Aplicar cor ao lucro líquido final (verde para positivo, vermelho para negativo)
        if (lucroLiquidoFinal >= 0) {
            textLucroLiquidoFinal.setTextColor(getColor(R.color.bar_color_due))
        } else {
            textLucroLiquidoFinal.setTextColor(getColor(R.color.md_theme_light_error))
        }
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
    
    private fun calculateDaysDifference(startDate: Date, endDate: Date): Int {
        val diffInMillis = endDate.time - startDate.time
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }
    
    private fun toggleSubFabs() {
        if (subFabOpen) {
            hideSubFabs()
            // Rotacionar ícone do FAB principal para posição inicial
            fabMenu.animate()
                .rotation(0f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        } else {
            showSubFabs()
            // Rotacionar ícone do FAB principal para indicar menu aberto
            fabMenu.animate()
                .rotation(45f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
        subFabOpen = !subFabOpen
    }
    
    private fun showSubFabs() {
        fabNovaDespesa.visibility = View.VISIBLE
        fabDownload.visibility = View.VISIBLE
        
        // Animações sequenciais conforme Material Design 3
        val animatorSet = AnimatorSet()
        
        val fabNovaDespesaAnimator = ObjectAnimator.ofPropertyValuesHolder(
            fabNovaDespesa,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
        ).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val fabDownloadAnimator = ObjectAnimator.ofPropertyValuesHolder(
            fabDownload,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
        ).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Sub-FABs aparecem em sequência (de baixo para cima)
        animatorSet.playSequentially(fabNovaDespesaAnimator, fabDownloadAnimator)
        animatorSet.start()
    }
    
    private fun hideSubFabs() {
        val animatorSet = AnimatorSet()
        
        val fabDownloadAnimator = ObjectAnimator.ofPropertyValuesHolder(
            fabDownload,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 1f, 0f)
        ).apply {
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    fabDownload.visibility = View.GONE
                }
            })
        }
        
        val fabNovaDespesaAnimator = ObjectAnimator.ofPropertyValuesHolder(
            fabNovaDespesa,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 1f, 0f)
        ).apply {
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    fabNovaDespesa.visibility = View.GONE
                }
            })
        }
        
        // Sub-FABs desaparecem em sequência (de cima para baixo)
        animatorSet.playSequentially(fabDownloadAnimator, fabNovaDespesaAnimator)
        animatorSet.start()
    }
    
    private fun setupDespesaRecyclerView() {
        despesaAdapter = DespesaAdapter(emptyList()) { despesa ->
            showDespesaOptionsDialog(despesa)
        }
        recyclerViewDespesas.layoutManager = LinearLayoutManager(this)
        recyclerViewDespesas.adapter = despesaAdapter
    }
    
    private fun loadDespesasList() {
        db.collection("despesas")
            .orderBy("dataTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val despesas = result.map { document ->
                    val data = document.getString("data") ?: "Data não disponível"
                    val descricao = document.getString("descricao") ?: "Despesa operacional"
                    val valor = document.getDouble("valor") ?: 0.0
                    
                    DespesaItem(
                        id = document.id,
                        data = data,
                        descricao = descricao,
                        valor = valor
                    )
                }
                despesaAdapter.updateDespesas(despesas)
            }
            .addOnFailureListener { e ->
                // Em caso de erro, manter lista vazia
                despesaAdapter.updateDespesas(emptyList())
            }
    }
    
    private fun showAddDespesaDialog() {
        // Criar layout para o modal
        val layoutInflater = LayoutInflater.from(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_despesa, null)
        
        val inputData = dialogView.findViewById<EditText>(R.id.inputData)
        val inputDespesa = dialogView.findViewById<EditText>(R.id.inputDespesa)
        val inputValor = dialogView.findViewById<EditText>(R.id.inputValor)
        
        // Aplicar formatação monetária automática no campo de valor
        MoneyTextWatcher.apply(inputValor)
        
        // Pré-preencher data atual
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        inputData.setText(currentDate)
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Adicionar Despesa")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val data = inputData.text.toString()
                val descricao = inputDespesa.text.toString()
                val valorStr = inputValor.text.toString()
                
                if (data.isEmpty() || descricao.isEmpty() || valorStr.isEmpty()) {
                    Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val valor = MoneyTextWatcher.getNumericValue(valorStr)
                if (valor <= 0) {
                    Toast.makeText(this, "Por favor, insira um valor válido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                addDespesa(data, descricao, valor)
            }
            .setNegativeButton("Cancelar", null)
            .create()
        
        // DialogUtils.styleAlertDialogButtons(dialog, this)
        dialog.show()
    }
    
    private fun addDespesa(data: String, descricao: String, valor: Double) {
        val despesaData = hashMapOf(
            "data" to data,
            "descricao" to descricao,
            "valor" to valor,
            "dataTimestamp" to System.currentTimeMillis()
        )
        
        db.collection("despesas")
            .add(despesaData)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Despesa adicionada com sucesso!", Toast.LENGTH_SHORT).show()
                loadFinancialMetrics() // Recarregar métricas
                loadDespesasList() // Recarregar lista de despesas
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao adicionar despesa: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    private fun downloadClientesList() {
        // Verificar permissões
        if (checkStoragePermission()) {
            proceedWithDownload()
        } else {
            requestStoragePermission()
        }
    }
    
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 e superior
            Environment.isExternalStorageManager()
        } else {
            // Android 10 e inferior
            val writePermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            writePermission == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 e superior - solicitar MANAGE_EXTERNAL_STORAGE
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Permissão Necessária")
                    .setMessage("Para salvar o arquivo Excel, é necessário permitir o acesso aos arquivos.")
                    .setPositiveButton("Conceder") { _, _ ->
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancelar", null)
                    .create()
                
                // DialogUtils.styleAlertDialogButtons(dialog, this)
                dialog.show()
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao solicitar permissão", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Android 10 e inferior
            storagePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }
    }
    
    private fun proceedWithDownload() {
        // Mostrar progresso
        val progressDialog = android.app.ProgressDialog(this).apply {
            setMessage("Gerando arquivo Excel...")
            setCancelable(false)
            show()
        }
        
        // Exportar para Excel
        ExcelExportUtil.exportClientesToExcel(this) { success, message, file ->
            progressDialog.dismiss()
            
            if (success && file != null) {
                // Mostrar opções ao usuário
                val dialog = android.app.AlertDialog.Builder(this)
                    .setTitle("Excel Criado com Sucesso!")
                    .setMessage("Arquivo salvo em: ${file.absolutePath}\n\nDeseja abrir o arquivo?")
                    .setPositiveButton("Abrir") { _, _ ->
                        ExcelExportUtil.openExcelFile(this, file)
                    }
                    .setNegativeButton("Fechar") { dialog, _ ->
                        dialog.dismiss()
                        Toast.makeText(this, "Arquivo salvo na pasta Downloads", Toast.LENGTH_LONG).show()
                    }
                    .create()
                
                // DialogUtils.styleAlertDialogButtons(dialog, this)
                dialog.show()
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showDespesaOptionsDialog(despesa: DespesaItem) {
        val options = arrayOf("Editar", "Deletar")
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Opções da Despesa")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showEditDespesaDialog(despesa) // Editar
                    1 -> showDeleteDespesaConfirmation(despesa) // Deletar
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        
        // DialogUtils.styleAlertDialogButtons(dialog, this)
        dialog.show()
    }
    
    private fun showEditDespesaDialog(despesa: DespesaItem) {
        val layoutInflater = LayoutInflater.from(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_despesa, null)
        
        val inputData = dialogView.findViewById<EditText>(R.id.inputData)
        val inputDespesa = dialogView.findViewById<EditText>(R.id.inputDespesa)
        val inputValor = dialogView.findViewById<EditText>(R.id.inputValor)
        
        // Aplicar formatação monetária automática no campo de valor
        MoneyTextWatcher.apply(inputValor)
        
        // Pré-preencher com dados existentes
        inputData.setText(despesa.data)
        inputDespesa.setText(despesa.descricao)
        inputValor.setText(MoneyTextWatcher.formatCurrency(despesa.valor))
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Despesa")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val data = inputData.text.toString()
                val descricao = inputDespesa.text.toString()
                val valorStr = inputValor.text.toString()
                
                if (data.isEmpty() || descricao.isEmpty() || valorStr.isEmpty()) {
                    Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val valor = MoneyTextWatcher.getNumericValue(valorStr)
                if (valor <= 0) {
                    Toast.makeText(this, "Por favor, insira um valor válido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                updateDespesa(despesa.id, data, descricao, valor)
            }
            .setNegativeButton("Cancelar", null)
            .create()
        
        // DialogUtils.styleAlertDialogButtons(dialog, this)
        dialog.show()
    }
    
    private fun updateDespesa(despesaId: String, data: String, descricao: String, valor: Double) {
        val despesaData = hashMapOf(
            "data" to data,
            "descricao" to descricao,
            "valor" to valor,
            "dataTimestamp" to System.currentTimeMillis()
        )
        
        db.collection("despesas")
            .document(despesaId)
            .update(despesaData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Despesa atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                loadFinancialMetrics()
                loadDespesasList()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao atualizar despesa: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    private fun showDeleteDespesaConfirmation(despesa: DespesaItem) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Deseja realmente excluir a despesa \"${despesa.descricao}\"?")
            .setPositiveButton("Sim") { _, _ ->
                deleteDespesa(despesa.id)
            }
            .setNegativeButton("Não", null)
            .create()
        
        // DialogUtils.styleAlertDialogButtons(dialog, this)
        dialog.show()
    }
    
    private fun deleteDespesa(despesaId: String) {
        db.collection("despesas")
            .document(despesaId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Despesa excluída com sucesso!", Toast.LENGTH_SHORT).show()
                loadFinancialMetrics()
                loadDespesasList()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao excluir despesa: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    // Função para zerar todos os registros de despesas (apenas para desenvolvimento/teste)
    private fun clearAllDespesas() {
        db.collection("despesas")
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                var count = 0
                
                for (document in result) {
                    batch.delete(document.reference)
                    count++
                }
                
                if (count > 0) {
                    batch.commit()
                        .addOnSuccessListener {
                            Toast.makeText(this, "$count despesas excluídas com sucesso!", Toast.LENGTH_LONG).show()
                            loadFinancialMetrics()
                            loadDespesasList()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Erro ao zerar despesas: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "Não há despesas para excluir", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar despesas: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    private fun loadDespesasFromFirebase(callback: (Double) -> Unit) {
        db.collection("despesas")
            .get()
            .addOnSuccessListener { result ->
                var totalDespesas = 0.0
                for (document in result) {
                    val valor = document.getDouble("valor") ?: 0.0
                    totalDespesas += valor
                }
                callback(totalDespesas)
            }
            .addOnFailureListener { e ->
                // Em caso de erro, usar 0.0
                callback(0.0)
            }
    }
    
    override fun onResume() {
        super.onResume()
        // Recarregar custo total e métricas ao retornar para a tela
        loadCustoTotalFromFirebase()
        loadFinancialMetrics()
        loadDespesasList()
        
        // Esconder sub-FABs se estiverem abertos
        if (subFabOpen) {
            hideSubFabs()
            // Rotacionar FAB principal de volta ao estado original
            fabMenu.rotation = 0f
            subFabOpen = false
        }
        
        // Animar entrada dos cards
        val cards = listOf<View>(
            findViewById(R.id.totalClientesCard),
            findViewById(R.id.contasAtivasCard),
            findViewById(R.id.contasVencidasCard),
            findViewById(R.id.contasAVencerCard),
            findViewById(R.id.contasInativasCard),
            findViewById(R.id.clientesGerandoReceitaCard),
            findViewById(R.id.totalVendasCard),
            findViewById(R.id.custoTotalCard),
            findViewById(R.id.lucroLiquidoCard),
            findViewById(R.id.despesasCard),
            findViewById(R.id.lucroLiquidoFinalCard),
            findViewById(R.id.margemLucroCard)
        )
        AnimationUtils.animateCardsEnter(cards, 100)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_theme -> {
                toggleTheme()
                true
            }
            R.id.menu_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleTheme() {
        showThemeMenu()
    }

    private fun showThemeMenu() {
        // Criar âncora para o popup (usar o botão de menu na toolbar)
        val anchor = toolbar.findViewById<View>(R.id.menu_theme) ?: toolbar
        
        val popupMenu = PopupMenu(this, anchor, android.view.Gravity.END)
        popupMenu.menuInflater.inflate(R.menu.theme_menu, popupMenu.menu)
        
        val currentTheme = ThemeUtils.getSavedThemeMode(this)
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> popupMenu.menu.findItem(R.id.menu_theme_auto)?.isChecked = true
            AppCompatDelegate.MODE_NIGHT_NO -> popupMenu.menu.findItem(R.id.menu_theme_light)?.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> popupMenu.menu.findItem(R.id.menu_theme_dark)?.isChecked = true
        }
        
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_theme_auto -> {
                    ThemeUtils.saveThemeMode(this, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    recreate()
                    true
                }
                R.id.menu_theme_light -> {
                    ThemeUtils.saveThemeMode(this, AppCompatDelegate.MODE_NIGHT_NO)
                    recreate()
                    true
                }
                R.id.menu_theme_dark -> {
                    ThemeUtils.saveThemeMode(this, AppCompatDelegate.MODE_NIGHT_YES)
                    recreate()
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }

    private fun logout() {
        val sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}