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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elevadorcom.ioritv.databinding.FragmentFinancasBinding
import com.elevadorcom.ioritv.utils.AccessibilityUtils
import com.elevadorcom.ioritv.utils.AnimationUtils
import com.elevadorcom.ioritv.utils.DialogUtils
import com.elevadorcom.ioritv.utils.MoneyTextWatcher
import com.elevadorcom.ioritv.utils.SituacaoConstants
import com.elevadorcom.ioritv.utils.SituacaoUtil
import com.elevadorcom.ioritv.utils.ThemeUtils
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Aba Finanças — conteúdo migrado da MainActivity4 (Fase 4).
 * Preserva: 12 cards de métricas, CRUD de despesas, custo total (edição),
 * exportação Excel, FABs com animação e acessibilidade dos cards.
 */
class FinancasFragment : Fragment() {

    private var _binding: FragmentFinancasBinding? = null
    private val binding get() = _binding!!

    private lateinit var despesaAdapter: DespesaAdapter

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
            Toast.makeText(requireContext(), "Permissão de armazenamento necessária para salvar o arquivo", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFinancasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Skin Glass: aplicar efeitos glassmorphism nos cards
        if (com.elevadorcom.ioritv.utils.ThemeUtils.isGlassEnabled(requireContext())) {
            com.elevadorcom.ioritv.utils.GlassUtils.applyGlassToFragment(view)
        }

        setupFloatingActionButtons()
        setupCustoTotalCard()
        setupDespesaRecyclerView()
        setupAccessibility()
        loadCustoTotalFromFirebase()
        loadFinancialMetrics()
        loadDespesasList()
    }

    override fun onResume() {
        super.onResume()
        if (_binding == null) return
        // Recarregar custo total e métricas ao retornar para a tela
        loadCustoTotalFromFirebase()
        loadFinancialMetrics()
        loadDespesasList()

        // Esconder sub-FABs se estiverem abertos
        if (subFabOpen) {
            hideSubFabs()
            fabMenu.rotation = 0f
            subFabOpen = false
        }

        // Animar entrada dos cards
        val cards = listOf<View>(
            binding.totalClientesCard,
            binding.contasAtivasCard,
            binding.contasVencidasCard,
            binding.contasAVencerCard,
            binding.contasInativasCard,
            binding.clientesGerandoReceitaCard,
            binding.totalVendasCard,
            binding.custoTotalCard,
            binding.lucroLiquidoCard,
            binding.despesasCard,
            binding.lucroLiquidoFinalCard,
            binding.margemLucroCard
        )
        AnimationUtils.animateCardsEnter(cards, 100)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val fabMenu: FloatingActionButton
        get() = binding.fabMenu

    private fun setupCustoTotalCard() {
        binding.custoTotalCard.setOnClickListener {
            AnimationUtils.animateButtonClick(it) {
                showEditCustoTotalDialog()
            }
        }
    }

    private fun setupFloatingActionButtons() {
        // FAB principal - toggle dos sub-FABs
        fabMenu.setOnClickListener {
            toggleSubFabs()
        }

        // Sub-FAB Nova Despesa
        binding.fabNovaDespesa.setOnClickListener {
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
        binding.fabDownload.setOnClickListener {
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

        binding.fabNovaDespesa.contentDescription = "Adicionar nova despesa"
        binding.fabNovaDespesa.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

        binding.fabDownload.contentDescription = "Download da lista de clientes"
        binding.fabDownload.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private fun setupAccessibility() {
        // Configurar acessibilidade para os cards - Seção Cadastros
        AccessibilityUtils.setupCardAccessibility(
            binding.totalClientesCard,
            "Card de total de clientes cadastrados"
        )

        AccessibilityUtils.setupCardAccessibility(
            binding.contasAtivasCard,
            "Card de contas ativas, clientes com créditos ativos adimplentes"
        )

        AccessibilityUtils.setupCardAccessibility(
            binding.contasVencidasCard,
            "Card de contas vencidas, clientes com créditos vencidos a mais de 15 dias"
        )

        AccessibilityUtils.setupCardAccessibility(
            binding.contasAVencerCard,
            "Card de contas a vencer, clientes com créditos a 3 dias do vencimento"
        )

        AccessibilityUtils.setupCardAccessibility(
            binding.contasInativasCard,
            "Card de contas inativas, clientes com créditos vencidos a mais de 30 dias"
        )

        AccessibilityUtils.setupCardAccessibility(
            binding.clientesGerandoReceitaCard,
            "Card de clientes gerando receita, total de clientes ativos e a vencer"
        )

        // Configurar acessibilidade para os cards - Seção Finanças
        AccessibilityUtils.setupCardAccessibility(
            binding.totalVendasCard,
            "Card de total de vendas, soma de todos os valores de clientes ativos"
        )

        AccessibilityUtils.setupCardAccessibility(
            binding.custoTotalCard,
            "Card de custo total operacional, toque para editar"
        )

        AccessibilityUtils.setupCardAccessibility(
            binding.lucroLiquidoCard,
            "Card de lucro líquido total, calculado como total de vendas menos custo total"
        )

        AccessibilityUtils.setupCardAccessibility(
            binding.despesasCard,
            "Card de despesas operacionais"
        )

        AccessibilityUtils.setupCardAccessibility(
            binding.lucroLiquidoFinalCard,
            "Card de lucro líquido final, calculado como lucro líquido total menos despesas"
        )

        AccessibilityUtils.setupCardAccessibility(
            binding.margemLucroCard,
            "Card de margem de lucro, percentual de lucro sobre vendas"
        )
    }

    private fun loadCustoTotalFromFirebase() {
        db.collection("configuracoes")
            .document("custoTotal")
            .get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
                custoTotalFixo = if (document.exists()) {
                    document.getDouble("valor") ?: 0.0
                } else {
                    0.0
                }
                binding.textCustoTotal.text = currencyFormat.format(custoTotalFixo)
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                custoTotalFixo = 0.0
                binding.textCustoTotal.text = currencyFormat.format(custoTotalFixo)
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
                if (_binding == null) return@addOnSuccessListener
                custoTotalFixo = valor
                binding.textCustoTotal.text = currencyFormat.format(valor)
                Toast.makeText(requireContext(), "Custo total atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                // Recarregar métricas para atualizar lucro
                loadFinancialMetrics()
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showEditCustoTotalDialog() {
        val input = EditText(requireContext())
        input.hint = "Digite o custo total"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        // Aplicar formatação monetária automática
        MoneyTextWatcher.apply(input)

        // Pré-preencher com valor atual
        if (custoTotalFixo > 0) {
            input.setText(MoneyTextWatcher.formatCurrency(custoTotalFixo))
        }

        val dialog = com.elevadorcom.ioritv.utils.DialogUtils.materialDialog(requireContext())
            .setTitle("Definir Custo Total")
            .setMessage("Informe o custo operacional mensal fixo:")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val valor = MoneyTextWatcher.getNumericValue(input.text.toString())

                if (valor >= 0) {
                    saveCustoTotalToFirebase(valor)
                } else {
                    Toast.makeText(requireContext(), "Por favor, insira um valor válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        if (ThemeUtils.isDarkTheme(requireContext())) {
            DialogUtils.styleAlertDialogButtonsDark(dialog, requireContext())
        } else {
            DialogUtils.styleAlertDialogButtons(dialog, requireContext())
        }
        dialog.show()
    }

    private fun loadFinancialMetrics() {
        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                // View pode ter sido destruída se o usuário trocou de aba
                if (_binding == null) return@addOnSuccessListener
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

                    // Regra canônica — Cloud Functions (calculateSituacao) é a fonte da verdade:
                    // ATIVO (≥3 dias), A VENCER (0..2), VENCIDO (−14..−1), STANDBY (≤ −15).
                    // SituacaoUtil aplica exatamente essas janelas (o MainActivity4 usava −30/−15/0..3,
                    // divergindo do SITUACAO gravado no Firestore pelas Cloud Functions).
                    val classificacao = if (terminoTimestamp != null) {
                        SituacaoUtil.calcularSituacao(terminoTimestamp.toDate())
                    } else {
                        situacao
                    }

                    when (classificacao) {
                        SituacaoConstants.STANDBY -> contasInativas++
                        SituacaoConstants.VENCIDO -> contasVencidas++
                        SituacaoConstants.A_VENCER -> {
                            contasAVencer++
                            // Incluir no total de vendas
                            totalVendas += valor
                        }
                        SituacaoConstants.ATIVO -> {
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
                    if (_binding == null) return@loadDespesasFromFirebase
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
                if (_binding == null) return@addOnFailureListener
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
        binding.textTotalClientes.text = totalClientes.toString()
        binding.textContasAtivas.text = contasAtivas.toString()
        binding.textContasVencidas.text = contasVencidas.toString()
        binding.textContasAVencer.text = contasAVencer.toString()
        binding.textContasInativas.text = contasInativas.toString()
        binding.textClientesGerandoReceita.text = clientesGerandoReceita.toString()

        // Seção Finanças
        binding.textTotalVendas.text = currencyFormat.format(totalVendas)
        binding.textCustoTotal.text = currencyFormat.format(custoTotal)
        binding.textLucroLiquido.text = currencyFormat.format(lucroLiquido)
        binding.textDespesas.text = currencyFormat.format(despesas)
        binding.textLucroLiquidoFinal.text = currencyFormat.format(lucroLiquidoFinal)
        binding.textMargemLucro.text = String.format(Locale.getDefault(), "%.2f%%", margemLucro)

        // Aplicar cor ao lucro líquido final (verde para positivo, vermelho para negativo)
        if (lucroLiquidoFinal >= 0) {
            binding.textLucroLiquidoFinal.setTextColor(requireContext().getColor(R.color.bar_color_due))
        } else {
            binding.textLucroLiquidoFinal.setTextColor(requireContext().getColor(R.color.md_theme_light_error))
        }
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
        binding.fabNovaDespesa.visibility = View.VISIBLE
        binding.fabDownload.visibility = View.VISIBLE

        // Animações sequenciais conforme Material Design 3
        val animatorSet = AnimatorSet()

        val fabNovaDespesaAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.fabNovaDespesa,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
        ).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
        }

        val fabDownloadAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.fabDownload,
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
            binding.fabDownload,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 1f, 0f)
        ).apply {
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    binding.fabDownload.visibility = View.GONE
                }
            })
        }

        val fabNovaDespesaAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.fabNovaDespesa,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 1f, 0f)
        ).apply {
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    binding.fabNovaDespesa.visibility = View.GONE
                }
            })
        }

        // Sub-FABs desaparecem em sequência (de cima para baixo)
        animatorSet.playSequentially(fabDownloadAnimator, fabNovaDespesaAnimator)
        animatorSet.start()
    }

    private fun setupDespesaRecyclerView() {
        val isGlass = com.elevadorcom.ioritv.utils.ThemeUtils.isGlassEnabled(requireContext())
        despesaAdapter = DespesaAdapter(emptyList(), { despesa ->
            showDespesaOptionsDialog(despesa)
        }, isGlass)
        binding.recyclerViewDespesas.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewDespesas.adapter = despesaAdapter
    }

    private fun loadDespesasList() {
        db.collection("despesas")
            .orderBy("dataTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                if (_binding == null) return@addOnSuccessListener
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
                if (_binding == null) return@addOnFailureListener
                // Em caso de erro, manter lista vazia
                despesaAdapter.updateDespesas(emptyList())
            }
    }

    private fun showAddDespesaDialog() {
        // Criar layout para o modal
        val layoutInflater = LayoutInflater.from(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_despesa, null)

        val inputData = dialogView.findViewById<EditText>(R.id.inputData)
        val inputDespesa = dialogView.findViewById<EditText>(R.id.inputDespesa)
        val inputValor = dialogView.findViewById<EditText>(R.id.inputValor)

        // Aplicar formatação monetária automática no campo de valor
        MoneyTextWatcher.apply(inputValor)

        // Pré-preencher data atual
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        inputData.setText(currentDate)

        val dialog = com.elevadorcom.ioritv.utils.DialogUtils.materialDialog(requireContext())
            .setTitle("Adicionar Despesa")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val data = inputData.text.toString()
                val descricao = inputDespesa.text.toString()
                val valorStr = inputValor.text.toString()

                if (data.isEmpty() || descricao.isEmpty() || valorStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val valor = MoneyTextWatcher.getNumericValue(valorStr)
                if (valor <= 0) {
                    Toast.makeText(requireContext(), "Por favor, insira um valor válido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                addDespesa(data, descricao, valor)
            }
            .setNegativeButton("Cancelar", null)
            .create()

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
                if (_binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Despesa adicionada com sucesso!", Toast.LENGTH_SHORT).show()
                loadFinancialMetrics() // Recarregar métricas
                loadDespesasList() // Recarregar lista de despesas
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Erro ao adicionar despesa: ${e.message}", Toast.LENGTH_LONG).show()
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
                requireContext(),
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
                val dialog = com.elevadorcom.ioritv.utils.DialogUtils.materialDialog(requireContext())
                    .setTitle("Permissão Necessária")
                    .setMessage("Para salvar o arquivo Excel, é necessário permitir o acesso aos arquivos.")
                    .setPositiveButton("Conceder") { _, _ ->
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancelar", null)
                    .create()

                dialog.show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao solicitar permissão", Toast.LENGTH_SHORT).show()
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
        val progressDialog = android.app.ProgressDialog(requireContext()).apply {
            setMessage("Gerando arquivo Excel...")
            setCancelable(false)
            show()
        }

        // Exportar para Excel
        ExcelExportUtil.exportClientesToExcel(requireContext()) { success, message, file ->
            if (!isAdded) return@exportClientesToExcel
            progressDialog.dismiss()

            if (success && file != null) {
                // Mostrar opções ao usuário
                val dialog = com.elevadorcom.ioritv.utils.DialogUtils.materialDialog(requireContext())
                    .setTitle("Excel Criado com Sucesso!")
                    .setMessage("Arquivo salvo em: ${file.absolutePath}\n\nDeseja abrir o arquivo?")
                    .setPositiveButton("Abrir") { _, _ ->
                        ExcelExportUtil.openExcelFile(requireContext(), file)
                    }
                    .setNegativeButton("Fechar") { dialog, _ ->
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "Arquivo salvo na pasta Downloads", Toast.LENGTH_LONG).show()
                    }
                    .create()

                dialog.show()
            } else {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDespesaOptionsDialog(despesa: DespesaItem) {
        val options = arrayOf("Editar", "Deletar")

        val dialog = com.elevadorcom.ioritv.utils.DialogUtils.materialDialog(requireContext())
            .setTitle("Opções da Despesa")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showEditDespesaDialog(despesa) // Editar
                    1 -> showDeleteDespesaConfirmation(despesa) // Deletar
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun showEditDespesaDialog(despesa: DespesaItem) {
        val layoutInflater = LayoutInflater.from(requireContext())
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

        val dialog = com.elevadorcom.ioritv.utils.DialogUtils.materialDialog(requireContext())
            .setTitle("Editar Despesa")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val data = inputData.text.toString()
                val descricao = inputDespesa.text.toString()
                val valorStr = inputValor.text.toString()

                if (data.isEmpty() || descricao.isEmpty() || valorStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val valor = MoneyTextWatcher.getNumericValue(valorStr)
                if (valor <= 0) {
                    Toast.makeText(requireContext(), "Por favor, insira um valor válido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updateDespesa(despesa.id, data, descricao, valor)
            }
            .setNegativeButton("Cancelar", null)
            .create()

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
                if (_binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Despesa atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                loadFinancialMetrics()
                loadDespesasList()
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Erro ao atualizar despesa: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDeleteDespesaConfirmation(despesa: DespesaItem) {
        val dialog = com.elevadorcom.ioritv.utils.DialogUtils.materialDialog(requireContext())
            .setTitle("Confirmar Exclusão")
            .setMessage("Deseja realmente excluir a despesa \"${despesa.descricao}\"?")
            .setPositiveButton("Sim") { _, _ ->
                deleteDespesa(despesa.id)
            }
            .setNegativeButton("Não", null)
            .create()

        dialog.show()
    }

    private fun deleteDespesa(despesaId: String) {
        db.collection("despesas")
            .document(despesaId)
            .delete()
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Despesa excluída com sucesso!", Toast.LENGTH_SHORT).show()
                loadFinancialMetrics()
                loadDespesasList()
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Erro ao excluir despesa: ${e.message}", Toast.LENGTH_LONG).show()
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
}
