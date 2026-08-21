package com.elevadorcom.ioritv

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.elevadorcom.ioritv.databinding.FragmentHomeBinding
import com.elevadorcom.ioritv.utils.AccessibilityUtils
import com.elevadorcom.ioritv.utils.AnimationUtils
import com.elevadorcom.ioritv.utils.PerformanceUtils
import com.elevadorcom.ioritv.utils.SituacaoConstants
import com.elevadorcom.ioritv.utils.SituacaoUtil
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.DocumentSnapshot

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt


/**
 * Aba Home — conteúdo migrado da RankingActivity (Fase 2).
 * Preserva: gráfico de barras por situação, progresso da carteira, slogan,
 * imagens de pódio, crédito total (com dialog de edição) e acessibilidade.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val sharedPreferences: SharedPreferences by lazy {
        requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Skin Glass: aplicar efeitos glassmorphism nos cards
        if (com.elevadorcom.ioritv.utils.ThemeUtils.isGlassEnabled(requireContext())) {
            com.elevadorcom.ioritv.utils.GlassUtils.applyGlassToFragment(view)
        }

        // Configurar acessibilidade
        setupAccessibility()

        // Inicializa as views e carrega dados
        // Configura o clique na TextView de total de créditos
        binding.totalCredit.setOnClickListener {
            showEditCreditsDialog()
        }

        // Carrega o valor inicial dos créditos da SharedPreferences
        val totalCredits = sharedPreferences.getInt("totalCredit", 150)
        binding.totalCredit.text = totalCredits.toString()

        // Atualiza ProgressBar e clientes
        updateProgressAndClientes()

        // Atualiza o card Pulse (saúde financeira)
        updatePulse()

        // Resumo rápido — badges de status
        updateStatusSummary()

        // CREDITOS
        updateTotalCredit()

        // PRD 6.3.2: Trail animation na barra de progresso
        startProgressTrailAnimation()
    }

    override fun onResume() {
        super.onResume()
        // Ensure the UI is updated when returning to this fragment
        if (_binding != null) {
            updateTotalCredit()
            // Fila de vencimentos — atualiza ao voltar do formulário (pós-renovação)
            updateRenewalsQueue()
            // Resumo rápido — badges de status
            updateStatusSummary()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        PerformanceUtils.cleanup()
        _binding = null
    }

    private fun updateTotalCredit() {
        val sharedPref = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val totalCreditValue = sharedPref.getInt("totalCredit", 150)
        binding.totalCredit.text = totalCreditValue.toString()
    }

    private fun updateProgressAndClientes() {
        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                // View pode ter sido destruída se o usuário trocou de aba
                if (_binding == null) return@addOnSuccessListener
                val totalClientes = result.size()
                binding.textTotalClientes.text = "Total de contas: $totalClientes"

                // Calcula a porcentagem e garante que não ultrapasse 100
                val percent = minOf(totalClientes * 100 / 150, 100)
                binding.progressBarClientes.progress = percent
                binding.textProgressPercent.text = "$percent%"
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                binding.textTotalClientes.text = "Erro ao contar clientes"
                binding.textProgressPercent.text = "Erro ao carregar"
            }
    }

    /**
     * Carrega os dados do Pulse (clientes, custo total e despesas) e renderiza o card.
     */
    private fun updatePulse() {
        db.collection("configuracoes").document("custoTotal").get()
            .addOnSuccessListener { custoDoc ->
                // View pode ter sido destruída se o usuário trocou de aba
                val custo = custoDoc.getDouble("valor") ?: 0.0
                db.collection("despesas").get()
                    .addOnSuccessListener { despesas ->
                        if (_binding == null) return@addOnSuccessListener
                        val despesasTotal = despesas.sumOf { it.getDouble("valor") ?: 0.0 }
                        db.collection("clientes").get()
                            .addOnSuccessListener { clientes ->
                                if (_binding == null) return@addOnSuccessListener
                                renderPulse(clientes, custo, despesasTotal)
                            }
                    }
            }
    }

    /**
     * Renderiza o card Pulse — Saúde Financeira.
     *
     * Score 0-100 (janelas canônicas das Cloud Functions via SituacaoUtil):
     *  - Adimplência (40%): ATIVO + A VENCER sobre o total de clientes;
     *  - Margem (30%): margem líquida, onde 30% = 100 pontos;
     *  - Receita (30%): receita sobre o custo total, onde cobrir 100% do custo = 100 pontos.
     * Cores do anel: verde (>= 80), âmbar (50-79), vermelho (< 50).
     */
    private fun renderPulse(clientes: QuerySnapshot, custo: Double, despesasTotal: Double) {
        var total = 0
        var adimplentes = 0
        var vendas = 0.0

        for (document in clientes) {
            total++
            val termino = document.getTimestamp("TERMINO")
            val situacao = document.getString("SITUACAO") ?: ""
            val classificacao = if (termino != null) {
                SituacaoUtil.calcularSituacao(termino.toDate())
            } else {
                situacao
            }
            if (classificacao == SituacaoConstants.ATIVO || classificacao == SituacaoConstants.A_VENCER) {
                adimplentes++
                vendas += document.getDouble("VALOR") ?: 0.0
            }
        }

        val adimplencia = if (total > 0) adimplentes * 100.0 / total else 0.0
        val adimplenciaInt = adimplencia.roundToInt()
        val margem = if (vendas > 0) (vendas - custo - despesasTotal) * 100.0 / vendas else 0.0

        binding.textPulseScore.text = "${adimplenciaInt}%"
        binding.textPulseLabel.text = when {
            adimplenciaInt >= 80 -> "Excelente"
            adimplenciaInt >= 50 -> "Em observação"
            else -> "Precisa de atenção"
        }
        binding.textPulseAdimplencia.text = "Margem: ${String.format(Locale.US, "%.1f%%", margem)}"
        binding.textPulseMargem.visibility = View.GONE
        val lucroLiquido = vendas - custo - despesasTotal
        binding.textPulseReceita.text = "Lucro Líquido: ${currencyFormat.format(lucroLiquido)}"

        val color = when {
            adimplenciaInt >= 80 -> ContextCompat.getColor(requireContext(), R.color.bar_color_due)
            adimplenciaInt >= 50 -> ContextCompat.getColor(requireContext(), R.color.bar_color_overdue)
            else -> ContextCompat.getColor(requireContext(), R.color.md_theme_light_error)
        }
        binding.pulseRing.setIndicatorColor(color)
        binding.pulseRing.setProgressCompat(adimplenciaInt, true)
        binding.pulseRing.contentDescription = "Adimplência: $adimplenciaInt%"
    }

    /**
     * Carrega a fila de vencimentos dos próximos 7 dias (0..7 dias até o TERMINO),
     * ordenada do mais urgente para o menos urgente, limitada a 7 chips.
     * Usa a mesma regra de dias do SituacaoUtil (fonte única com as Cloud Functions).
     */
    private fun updateRenewalsQueue() {
        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                // View pode ter sido destruída se o usuário trocou de aba
                if (_binding == null) return@addOnSuccessListener
                val proximos = result.documents
                    .mapNotNull { doc ->
                        val termino = doc.getTimestamp("TERMINO")?.toDate() ?: return@mapNotNull null
                        val dias = SituacaoUtil.diasParaVencimento(termino)
                        if (dias in 0..7) Pair(doc, dias) else null
                    }
                    .sortedBy { it.second }
                    .take(7)
                renderRenewalsQueue(proximos)
            }
    }

    /**
     * Renderiza os chips da fila: nome + contagem regressiva colorida + botão Renovar
     * (abre o ClienteFormFragment em modo edição). Sem vencimentos, mostra o estado vazio.
     *
     * Contagem regressiva colorida (semáforo):
     *  - 0 dias  = "Vence hoje"  → vermelho + pulso;
     *  - 1-2 dias = "Vence amanhã"/"Em X dias" → âmbar (janela A VENCER);
     *  - 3-7 dias = "Em X dias" → verde (janela ATIVO).
     */
    private fun renderRenewalsQueue(proximos: List<Pair<DocumentSnapshot, Int>>) {
        val container = binding.renewalsChips
        container.removeAllViews()

        if (proximos.isEmpty()) {
            binding.renewalsEmpty.visibility = View.VISIBLE
            binding.renewalsScroll.visibility = View.GONE
            binding.renewalsCount.visibility = View.GONE
            return
        }

        binding.renewalsEmpty.visibility = View.GONE
        binding.renewalsScroll.visibility = View.VISIBLE
        binding.renewalsCount.visibility = View.VISIBLE
        binding.renewalsCount.text = proximos.size.toString()

        val inflater = LayoutInflater.from(requireContext())
        for ((doc, dias) in proximos) {
            val chip = inflater.inflate(R.layout.chip_cliente_vencimento, container, false)
            val nome = doc.getString("NOME") ?: "Cliente"

            val countdown: String
            val cor: Int
            when {
                dias == 0 -> {
                    countdown = "Vence hoje"
                    cor = ContextCompat.getColor(requireContext(), R.color.md_theme_light_error)
                }
                dias in 1..2 -> {
                    // Janela A VENCER (0..2) — âmbar de alerta
                    countdown = if (dias == 1) "Vence amanhã" else "Em $dias dias"
                    cor = ContextCompat.getColor(requireContext(), R.color.bar_color_overdue)
                }
                else -> {
                    // Janela ATIVO (>= 3) — verde
                    countdown = "Em $dias dias"
                    cor = ContextCompat.getColor(requireContext(), R.color.bar_color_due)
                }
            }

            chip.findViewById<TextView>(R.id.chipNome).text = nome
            val countdownView = chip.findViewById<TextView>(R.id.chipCountdown)
            countdownView.text = countdown
            countdownView.setTextColor(cor)

            chip.findViewById<MaterialButton>(R.id.chipRenovar).setOnClickListener {
                // Abre o formulário em modo edição (mesmo fluxo do "Liberar TV")
                findNavController().navigate(
                    R.id.nav_cliente_form,
                    bundleOf("clienteId" to doc.id)
                )
            }
            chip.contentDescription = "$nome, $countdown"

            // Hoje = destaque pulsante (contagem regressiva vermelha)
            if (dias == 0) {
                AnimationUtils.pulse(countdownView, duration = 900, repeatCount = 3)
            }

            container.addView(chip)
        }
    }


    /**
     * Carrega clientes e renderiza os 4 badges de resumo rápido.
     * Cada badge é clicável e navega para a aba Carteiras com o filtro correspondente.
     */
    private fun updateStatusSummary() {
        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                if (_binding == null) return@addOnSuccessListener
                var countAtivo = 0
                var countAVencer = 0
                var countVencido = 0
                var countStandby = 0

                for (doc in result) {
                    val termino = doc.getTimestamp("TERMINO")?.toDate()
                    val situacao = if (termino != null) {
                        SituacaoUtil.calcularSituacao(termino)
                    } else {
                        doc.getString("SITUACAO") ?: ""
                    }
                    when (situacao) {
                        SituacaoConstants.ATIVO -> countAtivo++
                        SituacaoConstants.A_VENCER -> countAVencer++
                        SituacaoConstants.VENCIDO -> countVencido++
                        SituacaoConstants.STANDBY -> countStandby++
                    }
                }

                binding.countAtivo.text = countAtivo.toString()
                binding.countAVencer.text = countAVencer.toString()
                binding.countVencido.text = countVencido.toString()
                binding.countStandby.text = countStandby.toString()

                // Clicável → navega para Carteiras com filtro
                binding.badgeAtivo.setOnClickListener { navigateToFiltered("ATIVO") }
                binding.badgeAVencer.setOnClickListener { navigateToFiltered("A VENCER") }
                binding.badgeVencido.setOnClickListener { navigateToFiltered("VENCIDO") }
                binding.badgeStandby.setOnClickListener { navigateToFiltered("STANDBY") }
            }
    }

    private fun navigateToFiltered(filtro: String) {
        requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            .edit().putString("pendingFilter", filtro).apply()
        navigateToTab(R.id.nav_cadastros)
    }

    /**
     * Navega a uma aba usando as MESMAS opções da bottom nav (singleTop + popUpTo na raiz
     * com saveState + restoreState).
     */
    private fun navigateToTab(destino: Int) {
        val navController = findNavController()
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(navController.graph.findStartDestination().id, true)
            .build()
        navController.navigate(destino, null, options)
    }

    private fun showEditCreditsDialog() {
        // Infla o layout personalizado
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_edit_credits, null)

        // Acessa os componentes do layout
        val editText = dialogView.findViewById<EditText>(R.id.editCredits)
        editText.setText(sharedPreferences.getInt("totalCredit", 1).toString())

        // Cria e exibe a AlertDialog com o layout personalizado
        val dialog = com.elevadorcom.ioritv.utils.DialogUtils.materialDialog(requireContext())
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val newCredits = editText.text.toString().toIntOrNull() ?: return@setPositiveButton
                sharedPreferences.edit().putInt("totalCredit", newCredits).apply()
                binding.totalCredit.text = newCredits.toString()
                Toast.makeText(requireContext(), "Créditos atualizados para $newCredits", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .create()
        com.elevadorcom.ioritv.utils.DialogUtils.styleAlertDialogButtons(dialog, requireContext())
        dialog.show()
    }



    /**
     * Configura acessibilidade para todos os elementos
     */
    /**
     * PRD 6.3.2: Animação de flash na barra de progresso.
     * Um brilho horizontal percorre a extensão da porcentagem completada,
     * semelhante a um reflexo de luz sobre a barra colorida.
     * Loop infinito com delay entre ciclos.
     */
    private fun startProgressTrailAnimation() {
        val progressBar = binding.progressBarClientes
        progressBar.post {
            if (_binding == null) return@post

            val barHeight = progressBar.height
            val flashWidth = barHeight * 3  // Largura do flash proporcional à barra

            // View do flash: gradiente branco translúcido, ocupa toda a altura da barra
            val flashView = android.view.View(requireContext()).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 8f  // canto arredondado sutil
                    gradientType = android.graphics.drawable.GradientDrawable.LINEAR_GRADIENT
                    orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                    colors = intArrayOf(
                        0x00000000.toInt(),   // transparente
                        0x40FFFFFF.toInt(),   // branco 25%
                        0x80FFFFFF.toInt(),   // branco 50%
                        0x40FFFFFF.toInt(),   // branco 25%
                        0x00000000.toInt()    // transparente
                    )
                }
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    flashWidth,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = android.view.Gravity.NO_GRAVITY
                }
                alpha = 0f
            }

            val parent = progressBar.parent
            if (parent is android.widget.FrameLayout) {
                parent.clipChildren = false
                parent.addView(flashView)

                // Posiciona e dimensiona o flash para ocupar toda a altura da barra
                flashView.y = 0f
                flashView.x = progressBar.x
                val flashLp = flashView.layoutParams
                flashLp.height = barHeight
                flashView.layoutParams = flashLp

                fun animateFlash() {
                    if (_binding == null) return@animateFlash

                    // Flash percorre horizontalmente apenas a porcentagem completada
                    // Flash tem a altura total da barra (cima a baixo)
                    val progressFraction = progressBar.progress / 100f
                    val filledWidth = progressBar.width * progressFraction

                    flashView.x = progressBar.x - flashWidth.toFloat()  // começa antes da barra
                    flashView.animate()
                        .x(progressBar.x + filledWidth - flashWidth.toFloat())  // para na ponta do progresso
                        .alpha(1f)
                        .setDuration(800)
                        .setInterpolator(android.view.animation.LinearInterpolator())
                        .setUpdateListener { anim ->
                            val frac = anim.animatedFraction
                            flashView.alpha = when {
                                frac < 0.15f -> frac / 0.15f * 0.8f
                                frac > 0.85f -> (1f - frac) / 0.15f * 0.8f
                                else -> 0.8f
                            }
                        }
                        .withEndAction {
                            if (_binding != null) {
                                flashView.postDelayed({ animateFlash() }, 500)
                            }
                        }
                        .start()
                }

                // Inicia após um breve delay
                flashView.postDelayed({ animateFlash() }, 500)
            }
        }
    }

    private fun setupAccessibility() {
        // Configurar cards
        AccessibilityUtils.setupCardAccessibility(
            binding.cardView2,
            "Card de progresso da carteira de clientes"
        )

        AccessibilityUtils.setupCardAccessibility(
            binding.pulseCard,
            "Card de saúde financeira com anel de adimplência"
        )

        AccessibilityUtils.setupCardAccessibility(
            binding.renewalsCard,
            "Card de vencimentos próximos com botões de renovar"
        )

        AccessibilityUtils.setupCardAccessibility(
            binding.meusCreditos,
            "Card de créditos disponíveis",
            true
        )

        // Configurar descrições dinâmicas
        setupDynamicAccessibility()
    }

    /**
     * Configura descrições dinâmicas baseadas no estado atual
     */
    private fun setupDynamicAccessibility() {
        // Atualizar descrição do progresso
        val progressDescription = AccessibilityUtils.createProgressDescription(
            0, 100, "Progresso da carteira de clientes"
        )
        binding.progressBarClientes.contentDescription = progressDescription

        // Configurar feedback háptico para elementos importantes
        binding.meusCreditos.setOnClickListener {
            AccessibilityUtils.setupHapticFeedback(it)
        }
    }
}
