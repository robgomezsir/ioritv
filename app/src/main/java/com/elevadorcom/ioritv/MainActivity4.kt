package com.elevadorcom.ioritv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.elevadorcom.ioritv.utils.AccessibilityUtils
import com.elevadorcom.ioritv.utils.AnimationUtils

class MainActivity4 : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerViewDespesas: RecyclerView
    private lateinit var emptyMessage: TextView
    private lateinit var fabAddDespesa: FloatingActionButton
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main4)

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupFloatingActionButton()
        setupBottomNavigation()
        setupAccessibility()
    }
    
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerViewDespesas = findViewById(R.id.recyclerViewDespesas)
        emptyMessage = findViewById(R.id.emptyMessage)
        fabAddDespesa = findViewById(R.id.fabAddDespesa)
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Finanças"
    }
    
    private fun setupRecyclerView() {
        recyclerViewDespesas.layoutManager = LinearLayoutManager(this)
        recyclerViewDespesas.setHasFixedSize(true)
        
        // Por enquanto, mostrar mensagem vazia
        showEmptyState()
    }
    
    private fun setupFloatingActionButton() {
        fabAddDespesa.setOnClickListener {
            AnimationUtils.animateButtonClick(it) {
                // TODO: Implementar adição de despesa
                showAddDespesaDialog()
            }
        }
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
                    startActivity(Intent(this, MainActivity::class.java))
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
        // Configurar acessibilidade para os cards
        AccessibilityUtils.setupCardAccessibility(
            findViewById(R.id.resumoCard),
            "Card de resumo financeiro com totais de despesas e receitas"
        )
        
        AccessibilityUtils.setupCardAccessibility(
            findViewById(R.id.despesasCard),
            "Card de despesas recentes com lista de itens financeiros"
        )
        
        // Configurar acessibilidade para o FAB
        fabAddDespesa.contentDescription = "Botão para adicionar nova despesa"
        fabAddDespesa.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    
    private fun showEmptyState() {
        recyclerViewDespesas.visibility = View.GONE
        emptyMessage.visibility = View.VISIBLE
    }
    
    private fun hideEmptyState() {
        recyclerViewDespesas.visibility = View.VISIBLE
        emptyMessage.visibility = View.GONE
    }
    
    private fun showAddDespesaDialog() {
        // TODO: Implementar diálogo para adicionar despesa
        // Por enquanto, apenas uma animação de feedback
        AnimationUtils.pulse(fabAddDespesa, 500, 1)
    }
    
    override fun onResume() {
        super.onResume()
        // Animar entrada dos cards
        val cards = listOf<View>(
            findViewById(R.id.resumoCard),
            findViewById(R.id.despesasCard)
        )
        AnimationUtils.animateCardsEnter(cards, 150)
    }
}