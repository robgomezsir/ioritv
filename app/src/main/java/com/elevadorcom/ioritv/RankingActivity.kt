package com.elevadorcom.ioritv

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.util.TypedValue
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.elevadorcom.ioritv.databinding.ActivityRankingWithNavBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.google.firebase.auth.FirebaseAuth
import kotlin.random.Random
import android.app.AlertDialog
import android.content.SharedPreferences
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.getkeepsafe.taptargetview.TapTargetView
import com.google.android.material.appbar.MaterialToolbar
import com.elevadorcom.ioritv.utils.AnimationUtils
import com.elevadorcom.ioritv.utils.AccessibilityUtils
import com.elevadorcom.ioritv.utils.PerformanceUtils
import com.elevadorcom.ioritv.utils.ThemeUtils
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.FrameLayout

class RankingActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var binding: ActivityRankingWithNavBinding
    private var isLoading = true
    private lateinit var loadingView: View
    private lateinit var barChart: BarChart
    private lateinit var totalCredit: TextView
    private val sharedPreferences by lazy {
        getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    }
    private var loadingTasksCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica o tema apropriado usando ThemeUtils
        ThemeUtils.applyTheme(this)

        super.onCreate(savedInstanceState)
        // Inicializando o binding corretamente
        binding = ActivityRankingWithNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar a Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Meu Ioritv"

                // Configurar acessibilidade
                setupAccessibility()

        // Inicializar loading view
        initializeLoadingView()

        totalCredit = findViewById(R.id.totalCredit)

        // Mostra loading inicial
        showLoading()
        
        // Inicializa suas Views usando o binding e carrega dados
        setupViews()

        // Configuração da Barra de Navegação
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            handleNavigationItemSelected(item.itemId)
        }

        // Configura o clique na TextView de total de créditos
        binding.totalCredit.setOnClickListener {
            showEditCreditsDialog() // Chama o método para exibir o dialog de edição de créditos
        }

        // Carrega o valor inicial dos créditos da SharedPreferences
        val totalCredits = sharedPreferences.getInt("totalCredit", 150)
        binding.totalCredit.text = totalCredits.toString()

        val creditosASubtrair = intent.getIntExtra("creditosASubtrair", 0)

        if (creditosASubtrair > 0) {
            atualizarCreditos(totalCredits, creditosASubtrair)
        }

        // Obter a ImageView que está dentro do CardView
        val imageView = findViewById<ImageView>(R.id.iorinhoImageView)

        if (imageView != null) {
            // Lista de imagens na pasta drawable que representam o "iorinho"
            val iorinhoImages = listOf(
                R.drawable.iorinho_0,
                R.drawable.iorinho_1,
                R.drawable.iorinho_2,
                R.drawable.iorinho_3,
                R.drawable.iorinho_4,
                R.drawable.iorinho_5
            )

            // Selecionar uma imagem aleatória
            val randomImage = iorinhoImages[Random.nextInt(iorinhoImages.size)]

            // Definir a imagem aleatória na ImageView
            imageView.setImageResource(randomImage)
        } else {
            // Caso a ImageView não seja encontrada
            println("Erro: ImageView não encontrada dentro do CardView!")
        }

        // Acessando os elementos que serão destacados no tutorial
        val sloganTextView = findViewById<TextView>(R.id.sloganTextView4)
        val progressBarClientes = findViewById<ProgressBar>(R.id.progressBarClientes)
        val totalCredit = findViewById<TextView>(R.id.totalCredit)

        // Verificar se o tutorial já foi exibido antes usando SharedPreferences
        val sharedPref = getSharedPreferences("appPreferences", MODE_PRIVATE)
        val tutorialShown = sharedPref.getBoolean("tutorialShown", false)

        if (!tutorialShown) {
            // Exibir o tutorial usando TapTargetSequence
            iniciarTutorial(sharedPref)
        }

        // Atualiza ProgressBar e clientes
        updateProgressAndClientes()

        // Inicializa o gráfico de barras
        setupBarChart(isDarkMode())

        //CREDITOS
        updateTotalCredit()
    }
    override fun onResume() {
        super.onResume()
        updateTotalCredit() // Ensure the UI is updated when returning to this activity
    }

    private fun updateTotalCredit() {
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val totalCredit = sharedPref.getInt("totalCredit", 150)
        binding.totalCredit.text = totalCredit.toString()
    }

    private fun isDarkMode(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun setupViews() {
        val imageCenter = binding.imageCenter
        val imageLeft = binding.imageLeft
        val imageRight = binding.imageRight
        updateRankingImages(imageCenter, imageLeft, imageRight)
    }

    private fun updateRankingImages(imageCenter: ImageView, imageLeft: ImageView, imageRight: ImageView) {
        db.collection("clientes")
            .get()
            .addOnSuccessListener { documents ->
                val accountCount = documents.size()
                when {
                    accountCount <= 50 -> {
                        imageCenter.setImageResource(R.drawable.first_place_image)
                        imageLeft.setImageResource(R.drawable.third_place_image)
                        imageRight.setImageResource(R.drawable.second_place_image)
                    }
                    accountCount <= 100 -> {
                        imageCenter.setImageResource(R.drawable.second_place_image)
                        imageLeft.setImageResource(R.drawable.first_place_image)
                        imageRight.setImageResource(R.drawable.third_place_image)
                    }
                    else -> {
                        imageCenter.setImageResource(R.drawable.third_place_image)
                        imageLeft.setImageResource(R.drawable.second_place_image)
                        imageRight.setImageResource(R.drawable.first_place_image)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao atualizar imagens", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleNavigationItemSelected(itemId: Int): Boolean {
        return when (itemId) {
            R.id.nav_home -> true
            R.id.nav_cadastros -> {
                startActivity(Intent(this, MainActivity2::class.java))
                true
            }
            R.id.nav_financas -> {
                startActivity(Intent(this, MainActivity4::class.java))
                true
            }
            else -> false
        }
    }

    private fun updateProgressAndClientes() {
        startLoadingTask()
        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                val totalClientes = result.size()
                binding.textTotalClientes.text = "Total de contas: $totalClientes"

                // Calcula a porcentagem e garante que não ultrapasse 100
                val percent = minOf(totalClientes * 100 / 150, 100)
                binding.progressBarClientes.progress = percent
                binding.textProgressPercent.text = "$percent%"

                // Lógica para atualizar o slogan
                val slogan = when {
                    totalClientes in 1..50 -> "Tem a sua cara!"
                    totalClientes in 51..100 -> "É coisa nossa!"
                    totalClientes > 100 -> "A gente se vê por aqui!"
                    else -> ""
                }
                binding.sloganTextView4.text = slogan
                finishLoadingTask()
            }
            .addOnFailureListener {
                binding.textTotalClientes.text = "Erro ao contar clientes"
                binding.textProgressPercent.text = "Erro ao carregar"
                finishLoadingTask()
            }
    }

    private fun setupBarChart(isDarkMode: Boolean) {
        barChart = binding.barChart

        // Ajustar a distância entre os rótulos do eixo X e a parte inferior do gráfico (dentro da área de plotagem)
        barChart.xAxis.yOffset = 0.5f  // Valor negativo diminui a distância; ajuste conforme necessário

        // Configuração básica do gráfico
        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        barChart.animateY(1000)

        // Remover linhas de grade do eixo Y
        barChart.axisLeft.setDrawGridLines(false)
        barChart.axisRight.setDrawGridLines(false)

        // Remover rótulos e linha do eixo Y
        barChart.axisLeft.setDrawLabels(false)  // Remove os rótulos do eixo Y esquerdo
        barChart.axisLeft.setDrawAxisLine(false)  // Remove a linha do eixo Y esquerdo
        barChart.axisRight.setDrawLabels(false)  // Remove os rótulos do eixo Y direito
        barChart.axisRight.setDrawAxisLine(false)  // Remove a linha do eixo Y direito

        barChart.legend.isEnabled = false
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.setDrawGridLines(false)
        barChart.xAxis.granularity = 1f

        // Definir as cores para o modo escuro ou claro
        val axisColor = if (isDarkMode) Color.WHITE else Color.BLACK

        // Definir cores dos eixos X
        barChart.xAxis.textColor = axisColor
        barChart.xAxis.textSize = 12f // Defina o valor que você quiser, como 12f para 12sp

        // Limitar altura máxima do eixo Y
        barChart.axisLeft.axisMinimum = 0f  // Limite inferior do eixo Y
        barChart.axisRight.axisMinimum = 0f
        barChart.axisLeft.axisMaximum = 100f  // Limite superior do eixo Y
        barChart.axisRight.axisMaximum = 100f

        barChart.renderer = RoundedBarChartRenderer(barChart, barChart.animator, barChart.viewPortHandler)

        // Recuperar dados do Firestore
        startLoadingTask()
        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                var countAtivo = 0
                var countAVencer = 0
                var countVencido = 0
                var countStandby = 0

                // Contagem de clientes por situação
                for (document in result) {
                    when (document.getString("SITUACAO")) {
                        "ATIVO" -> countAtivo++
                        "A VENCER" -> countAVencer++
                        "VENCIDO" -> countVencido++
                        "STANDBY" -> countStandby++
                    }
                }

                // Definir as entradas do gráfico
                val entries = listOf(
                    BarEntry(0f, countAtivo.toFloat()),
                    BarEntry(1f, countAVencer.toFloat()),
                    BarEntry(2f, countVencido.toFloat()),
                    BarEntry(3f, countStandby.toFloat())
                )

                // Definir as cores das barras de acordo com a situação dos clientes
                val dataSet = BarDataSet(entries, "Situação dos Clientes").apply {
                    colors = listOf(
                        ContextCompat.getColor(this@RankingActivity, R.color.bar_color_active),
                        ContextCompat.getColor(this@RankingActivity, R.color.bar_color_due),
                        ContextCompat.getColor(this@RankingActivity, R.color.bar_color_overdue),
                        ContextCompat.getColor(this@RankingActivity, R.color.bar_color_standby)
                    )
                    barShadowColor = Color.TRANSPARENT
                    setDrawValues(true)
                    valueTextSize = 10f
                    valueFormatter = DefaultValueFormatter(0)
                }

                // Aplicar os dados e configurar o gráfico
                val data = BarData(dataSet).apply { barWidth = 0.5f }
                barChart.data = data

                // Definir os rótulos do eixo X (situação dos clientes)
                barChart.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Ativo", "A Vencer", "Vencido", "Standby"))
                barChart.invalidate() // Atualiza o gráfico com os novos dados
                finishLoadingTask()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar dados: ${e.message}", Toast.LENGTH_LONG).show()
                finishLoadingTask()
            }
    }

    class RoundedBarChartRenderer(
        chart: BarChart,
        animator: com.github.mikephil.charting.animation.ChartAnimator,
        viewPortHandler: ViewPortHandler
    ) : BarChartRenderer(chart, animator, viewPortHandler) {

        private val radius = 50f

        override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
            val trans = mChart.getTransformer(dataSet.axisDependency)
            val barWidthHalf = mChart.barData.barWidth / 2

            for (j in 0 until dataSet.entryCount) {
                val entry = dataSet.getEntryForIndex(j)

                val rectF = RectF(
                    entry.x - barWidthHalf, 0f,
                    entry.x + barWidthHalf, entry.y
                )

                trans.rectToPixelPhase(rectF, mAnimator.phaseY)

                mRenderPaint.color = dataSet.getColor(j % dataSet.colors.size)

                if (rectF.height() > 0) {
                    c.drawRoundRect(rectF, radius, radius, mRenderPaint)

                    mRenderPaint.textSize = 30f  // Adjust as needed

                    val valueText = entry.y.toInt().toString()
                    val x = rectF.centerX()
                    val y = rectF.top - 10
                    c.drawText(valueText, x, y, mRenderPaint)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_theme -> {
                // Ação para "Tema" - Alterna entre tema claro e escuro
                toggleTheme()
                true
            }

            R.id.menu_tutorial -> {
                // Ação para Iniciar Tutorial
                iniciarTutorial(sharedPreferences)
                true
            }

            R.id.menu_logout -> {
                // Ação para "sair"
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
        val popupMenu = PopupMenu(this, binding.toolbar)
        popupMenu.menuInflater.inflate(R.menu.theme_menu, popupMenu.menu)
        
        // Marcar o tema atual como selecionado
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
        // Limpa o estado de login nas SharedPreferences
        val sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear() // Limpa todas as preferências
        editor.apply()

        // Redireciona para a LoginActivity
        startActivity(Intent(this, LoginActivity::class.java))
        finish() // Finaliza a RankingActivity para que o usuário não possa voltar
    }

    private fun showEditCreditsDialog() {
        // Infla o layout personalizado
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_edit_credits, null)

        // Acessa os componentes do layout
        val editText = dialogView.findViewById<EditText>(R.id.editCredits)
        editText.setText(sharedPreferences.getInt("totalCredit", 1).toString())

        // Cria e exibe a AlertDialog com o layout personalizado
        AlertDialog.Builder(this)
            .setView(dialogView) // Define o layout personalizado
            .setPositiveButton("Salvar") { _, _ ->
                val newCredits = editText.text.toString().toIntOrNull() ?: return@setPositiveButton
                sharedPreferences.edit().putInt("totalCredit", newCredits).apply()
                binding.totalCredit.text = newCredits.toString()
                Toast.makeText(this, "Créditos atualizados para $newCredits", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null) // Botão de cancelar
            .create()
            .show()
    }

    private fun atualizarCreditos(totalCredits: Int, creditosASubtrair: Int) {
        // Subtrai os créditos
        val novosCreditos = totalCredits - creditosASubtrair
        binding.totalCredit.text = novosCreditos.toString()

        // Atualiza o valor na TextView
        totalCredit.text = novosCreditos.toString()

        // Salva o novo valor nos SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putInt("totalCredit", novosCreditos)
        editor.apply()

        Toast.makeText(this, "Créditos atualizados: $novosCreditos", Toast.LENGTH_SHORT).show()
    }

    private fun iniciarTutorial(sharedPref: SharedPreferences) {
        TapTargetSequence(this)
            .targets(
                // Destaque da toolbar
                TapTarget.forView(binding.toolbar, "Suas configurações gerais", "Acesse aqui as configurações gerais do aplicativo através do menu.")
                    .outerCircleColor(R.color.md_theme_light_primary)
                    .targetCircleColor(R.color.md_theme_light_primaryContainer)
                    .titleTextSize(20)
                    .descriptionTextSize(15)
                    .cancelable(false),

                // Destaque do card de ranking
                TapTarget.forView(binding.rankingCard, "Ranking de Performance", "Aqui você pode ver seu ranking baseado no número de clientes.")
                    .outerCircleColor(R.color.md_theme_light_primary)
                    .targetCircleColor(R.color.md_theme_light_primaryContainer)
                    .titleTextSize(20)
                    .descriptionTextSize(15)
                    .cancelable(false),

                // Destaque da barra de progresso de clientes
                TapTarget.forView(binding.progressBarClientes, "Alcance de sua meta", "Acompanhe aqui o progresso para alcançar sua meta de clientes.")
                    .outerCircleColor(R.color.md_theme_light_primary)
                    .targetCircleColor(R.color.md_theme_light_primaryContainer)
                    .titleTextSize(20)
                    .descriptionTextSize(15)
                    .cancelable(false),

                // Destaque do campo de créditos
                TapTarget.forView(binding.totalCredit, "Adicione ou altere seus créditos", "Gerencie os créditos diretamente por aqui.")
                    .outerCircleColor(R.color.md_theme_light_primary)
                    .targetCircleColor(R.color.md_theme_light_primaryContainer)
                    .titleTextSize(20)
                    .descriptionTextSize(15)
                    .cancelable(false),

                // Destaque do Iorinho
                TapTarget.forView(binding.iorinhoImageView, "Seu mascote Iorinho está aqui!", "O Iorinho muda sua pose a cada abertura do aplicativo.")
                    .outerCircleColor(R.color.md_theme_light_primary)
                    .targetCircleColor(R.color.md_theme_light_primaryContainer)
                    .titleTextSize(20)
                    .descriptionTextSize(15)
                    .cancelable(false),

                // Destaque do gráfico
                TapTarget.forView(binding.barChart, "Status dos Clientes", "Visualize o status de todos os seus clientes em um gráfico.")
                    .outerCircleColor(R.color.md_theme_light_primary)
                    .targetCircleColor(R.color.md_theme_light_primaryContainer)
                    .titleTextSize(20)
                    .descriptionTextSize(15)
                    .cancelable(false)

            )
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceFinish() {
                    // Salvar no SharedPreferences que o tutorial foi exibido
                    sharedPref.edit().putBoolean("tutorialShown", true).apply()
                }

                override fun onSequenceStep(lastTarget: TapTarget, targetClicked: Boolean) {}
                override fun onSequenceCanceled(lastTarget: TapTarget) {}
            })
            .start()
    }

    /**
     * Inicializa a view de loading
     */
    private fun initializeLoadingView() {
        loadingView = LayoutInflater.from(this).inflate(R.layout.loading_ranking, null)
        
        // Adiciona a loading view como overlay
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val loadingContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(loadingView)
        }
        rootView.addView(loadingContainer)
    }

    /**
     * Mostra o estado de loading
     */
    private fun startLoadingTask() {
        loadingTasksCount++
        if (!isLoading) {
            showLoading()
        }
    }
    
    private fun finishLoadingTask() {
        loadingTasksCount--
        if (loadingTasksCount <= 0) {
            loadingTasksCount = 0
            hideLoading()
        }
    }
    
    private fun showLoading() {
        isLoading = true
        loadingView.visibility = View.VISIBLE
        binding.root.visibility = View.GONE
    }

    /**
     * Esconde o estado de loading e mostra o conteúdo com animação
     */
    private fun hideLoading() {
        if (!isLoading) return
        
        isLoading = false
        AnimationUtils.transitionFromLoadingToContent(loadingView, binding.root)
        
        // Anima a entrada dos cards
        val cards = listOf(
            binding.rankingCard,
            binding.cardView2,
            binding.meusCreditos,
            binding.cardView4,
            binding.cardView5
        )
        
        AnimationUtils.animateCardsEnter(cards, 100)
    }


    /**
     * Configura acessibilidade para todos os elementos
     */
    private fun setupAccessibility() {
        // Configurar cards
        AccessibilityUtils.setupCardAccessibility(
            binding.rankingCard,
            "Card de ranking de performance com posições de pódio"
        )
        
        AccessibilityUtils.setupCardAccessibility(
            binding.cardView2,
            "Card de progresso da carteira de clientes"
        )
        
        AccessibilityUtils.setupCardAccessibility(
            binding.meusCreditos,
            "Card de créditos disponíveis",
            true
        )
        
        AccessibilityUtils.setupCardAccessibility(
            binding.cardView4,
            "Card do mascote Iorinho"
        )
        
        AccessibilityUtils.setupCardAccessibility(
            binding.cardView5,
            "Card do gráfico de status dos clientes"
        )

        // Configurar navegação sequencial
        val focusableViews = listOf(
            binding.rankingCard,
            binding.cardView2,
            binding.meusCreditos,
            binding.cardView4,
            binding.cardView5,
            binding.bottomNavigation
        )
        AccessibilityUtils.setupSequentialNavigation(focusableViews)

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

    /**
     * Otimiza performance da atividade
     */
    private fun optimizePerformance() {
        // Otimizar RecyclerView se houver
        // PerformanceUtils.optimizeRecyclerView(binding.recyclerView)
        
        // Configurar lazy loading se necessário
        // PerformanceUtils.setupLazyLoading(binding.recyclerView) {
        //     // Carregar mais dados
        // }
        
        // Monitorar uso de memória
        PerformanceUtils.executeInBackground {
            val memoryInfo = PerformanceUtils.getMemoryUsage()
            if (memoryInfo.isMemoryLow()) {
                PerformanceUtils.forceGarbageCollectionIfNeeded()
            }
            if (memoryInfo.isMemoryCritical()) {
                PerformanceUtils.clearImageCacheIfNeeded()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpar recursos de performance
        PerformanceUtils.cleanup()
    }

}