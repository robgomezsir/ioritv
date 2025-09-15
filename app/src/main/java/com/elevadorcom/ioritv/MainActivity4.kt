package com.elevadorcom.ioritv

import DespesasAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.gridlayout.widget.GridLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import android.Manifest
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.FirebaseApp

class MainActivity4 : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var gridLayout: GridLayout
    private lateinit var recyclerViewDespesas: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var despesasAdapter: DespesasAdapter
    private lateinit var fabMenu: FloatingActionButton
    private lateinit var fabDespesa: FloatingActionButton
    private lateinit var fabDownload: FloatingActionButton

    // Registro para selecionar o diretório para salvar o arquivo
    private val selectDirectoryLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            uri?.let { saveCsvToDirectory(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar o tema apropriado com base no modo da noite
        val isDarkMode = true
        if (isDarkMode) {
            setTheme(R.style.Theme_IORITv_MainActivity2_Dark)
        } else {
            setTheme(R.style.Base_Theme_IORITv_Dark)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main4)

        FirebaseApp.initializeApp(this)
        firestore = FirebaseFirestore.getInstance()
        gridLayout = findViewById(R.id.gridLayout)
        recyclerViewDespesas = findViewById(R.id.recyclerViewDespesas)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Configurar o RecyclerView
        despesasAdapter = DespesasAdapter(this, ::onEditDespesa, ::onDeleteDespesa)
        recyclerViewDespesas.layoutManager = LinearLayoutManager(this)
        recyclerViewDespesas.adapter = despesasAdapter
        fabMenu = findViewById(R.id.fabMenu)
        fabDespesa = findViewById(R.id.fabDespesa)
        fabDownload = findViewById(R.id.fabDownload)

        fabMenu.setOnClickListener {
            toggleFabMenu()
        }

        // Configurar o FAB para adicionar nova despesa
        fabDespesa.setOnClickListener {
            startActivity(Intent(this, DespesasActivity::class.java))
        }

        // Configurar o Download da coleção "clientes"
        fabDownload.setOnClickListener {
            // Solicitar ao usuário para selecionar o diretório
            selectDirectoryLauncher.launch(null)
        }

        // Configurar o SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            loadAndCalculateData()
        }

        // Carregar dados e calcular inicialmente
        loadAndCalculateData()

        // Configurar a barra de navegação
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, RankingActivity::class.java))
                    true
                }

                R.id.nav_cadastros -> {
                    startActivity(Intent(this, MainActivity2::class.java))
                    true
                }

                R.id.nav_financas -> {
                    // Já está na tela de despesas
                    true
                }

                else -> false
            }
        }

        // Manter a seleção do menu atual
        bottomNav.selectedItemId = R.id.nav_financas
    }

    private fun loadAndCalculateData() {
        firestore.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                var totalVendas = 0.0
                var totalClientes = 0
                var totalVencidos = 0
                var totalAVencer = 0
                var totalAtivos = 0
                var totalStandby = 0
                var valorStandby = 0.0

                for (document in result) {
                    totalClientes++
                    val valor = document.getDouble("VALOR") ?: 0.0
                    val desconto = document.getDouble("DESCONTO") ?: 0.0
                    val situacao = document.getString("SITUACAO")

                    totalVendas += (valor - desconto)

                    when (situacao) {
                        "VENCIDO" -> totalVencidos++
                        "A VENCER" -> totalAVencer++
                        "ATIVO" -> totalAtivos++
                        "STANDBY" -> {
                            totalStandby++
                            valorStandby += valor
                        }
                    }
                }

                firestore.collection("despesas")
                    .get()
                    .addOnSuccessListener { despesasResult ->
                        var totalDespesas = 0.0

                        for (doc in despesasResult) {
                            val valorDespesa = doc.getDouble("Valor") ?: 0.0
                            totalDespesas += valorDespesa
                        }

                        val custoFixo = 850.0
                        val lucroTotal = totalVendas - custoFixo - valorStandby
                        val lucroFinal = lucroTotal - totalDespesas

                        displayData(
                            totalClientes,
                            totalVencidos,
                            totalAVencer,
                            totalAtivos,
                            totalStandby,
                            totalVendas,
                            custoFixo,
                            lucroTotal,
                            totalDespesas,
                            lucroFinal
                        )
                        despesasAdapter.setDespesas(despesasResult.documents)

                        // Parar o carregamento do SwipeRefreshLayout
                        swipeRefreshLayout.isRefreshing = false
                    }
                    .addOnFailureListener {
                        // Mostrar erro se falhar
                        swipeRefreshLayout.isRefreshing = false
                    }
            }
    }

    private fun displayData(
        totalClientes: Int,
        totalVencidos: Int,
        totalAVencer: Int,
        totalAtivos: Int,
        totalStandby: Int,
        totalVendas: Double,
        custoFixo: Double,
        lucroTotal: Double,
        totalDespesas: Double,
        lucroFinal: Double
    ) {
        gridLayout.removeAllViews()

        addCard("Total de Clientes", totalClientes.toString())
        addCard("Contas Vencidas", totalVencidos.toString())
        addCard("Contas a Vencer", totalAVencer.toString())
        addCard("Contas Ativas", totalAtivos.toString())
        addCard("Contas Inativas", totalStandby.toString())
        addCard("Total de Vendas", formatCurrency(totalVendas))
        addCard("Custo Fixo", formatCurrency(custoFixo))
        addCard("Lucro Total", formatCurrency(lucroTotal))
        addCard("Despesas", formatCurrency(totalDespesas))
        addCard("Lucro Final", formatCurrency(lucroFinal))
    }

    private fun addCard(title: String, value: String) {
        val cardView = LayoutInflater.from(this)
            .inflate(R.layout.item_financeiro, gridLayout, false) as CardView
        val textViewTitulo = cardView.findViewById<TextView>(R.id.textViewTitulo)
        val textViewValor = cardView.findViewById<TextView>(R.id.textViewValor)

        textViewTitulo.text = title
        textViewValor.text = value

        val layoutParams = GridLayout.LayoutParams()
        layoutParams.width = 0 // Ocupa o espaço disponível
        layoutParams.height = GridLayout.LayoutParams.WRAP_CONTENT
        layoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        layoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
        layoutParams.setMargins(8, 8, 8, 8)

        cardView.layoutParams = layoutParams
        gridLayout.addView(cardView)
    }

    private fun formatCurrency(amount: Double): String {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return numberFormat.format(amount)
    }

    private fun onEditDespesa(despesa: DocumentSnapshot) {
        val dataTimestamp = despesa.getTimestamp("Data")
        val dataString = dataTimestamp?.toDate()?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
        } // Converte o Timestamp para String no formato "dd/MM/yyyy"

        val valor = despesa.getDouble("Valor") ?: 0.0
        val valorFormatado = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

        val intent = Intent(this, DespesasActivity::class.java).apply {
            putExtra("DESPESA_ID", despesa.id)
            putExtra("DESPESA_VALOR", valor)
            putExtra("DESPESA_DATA", dataString)
        }
        startActivity(intent)
    }

    private fun onDeleteDespesa(despesa: DocumentSnapshot) {
        val despesaId = despesa.id
        firestore.collection("despesas").document(despesaId).delete()
            .addOnSuccessListener {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Despesa excluída com sucesso!",
                    Snackbar.LENGTH_LONG
                ).show()
                loadAndCalculateData()
            }
    }

    private fun toggleFabMenu() {
        if (fabDespesa.visibility == View.VISIBLE) {
            fabDespesa.visibility = View.GONE
            fabDownload.visibility = View.GONE
        } else {
            fabDespesa.visibility = View.VISIBLE
            fabDownload.visibility = View.VISIBLE
        }
    }

    // Função para salvar o arquivo CSV
    private fun saveCsvToDirectory(uri: Uri) {
        val filename = "relatorio_clientes.csv"
        val contentBuilder = StringBuilder()

        // Cabeçalho do CSV
        contentBuilder.append("CODIGO,NOME,USUARIO,SENHA,WHATSAPP,MODELO,INICIO,CREDITOS,TERMINO,VENCIMENTO,SITUACAO,MAC,OTP,DEVICE,VALOR,CUSTO,DESCONTO,SERVIDOR\n")

        firestore.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val codigo = document.id
                    val nome = document.getString("NOME") ?: ""
                    val usuario = document.getString("USUARIO") ?: ""
                    val senha = document.getString("SENHA") ?: ""
                    val whatsapp = document.getString("WHATSAPP") ?: ""
                    val modelo = document.getString("MODELO") ?: ""
                    val inicio = document.getTimestamp("INICIO")?.toDate()?.let {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                    } ?: ""
                    val creditos = document.getDouble("CREDITOS") ?: 0.0
                    val termino = document.getTimestamp("TERMINO")?.toDate()?.let {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                    } ?: ""
                    val vencimento = document.getString("VENCIMENTO") ?: ""
                    val situacao = document.getString("SITUACAO") ?: ""
                    val mac = document.getString("MAC") ?: ""
                    val otp = document.getString("OTP") ?: ""
                    val device = document.getString("DEVICE") ?: ""
                    val valor = document.getDouble("VALOR") ?: 0.0
                    val custo = document.getDouble("CUSTO") ?: 0.0
                    val desconto = document.getDouble("DESCONTO") ?: 0.0
                    val servidor = document.getString("SERVIDOR") ?: ""

                    contentBuilder.append("$codigo,$nome,$usuario,$senha,$whatsapp,$modelo,$inicio,$creditos,$termino,$vencimento,$situacao,$mac,$otp,$device,$valor,$custo,$desconto,$servidor\n")
                }

                try {
                    val contentResolver = contentResolver
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                        uri,
                        DocumentsContract.getTreeDocumentId(uri)
                    )
                    val newFileUri = DocumentsContract.createDocument(
                        contentResolver,
                        documentUri,
                        "text/csv",
                        filename
                    )

                    newFileUri?.let {
                        contentResolver.openFileDescriptor(it, "w")?.use { pfd ->
                            FileOutputStream(pfd.fileDescriptor).use { outputStream ->
                                outputStream.write(contentBuilder.toString().toByteArray())
                            }
                        }
                        Snackbar.make(
                            recyclerViewDespesas,
                            "Arquivo salvo com sucesso!",
                            Snackbar.LENGTH_LONG
                        ).show()
                    } ?: run {
                        Snackbar.make(
                            recyclerViewDespesas,
                            "Falha ao salvar o arquivo",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    Snackbar.make(
                        recyclerViewDespesas,
                        "Erro ao salvar o arquivo: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Snackbar.make(
                    recyclerViewDespesas,
                    "Erro ao acessar clientes: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
    }
}