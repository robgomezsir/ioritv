package com.elevadorcom.ioritv

import ClienteAdapter
import UpdateWorker

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.elevadorcom.ioritv.databinding.ActivityMain2Binding
import com.google.firebase.firestore.Query
// import com.elevadorcom.ioritv.utils.DialogUtils
import com.elevadorcom.ioritv.utils.ThemeUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityMain2Binding
    private var clienteList: List<DocumentSnapshot> = emptyList()
    private val db = FirebaseFirestore.getInstance()
    private val filterStates = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica o tema apropriado usando ThemeUtils
        ThemeUtils.applyTheme(this)
        
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a toolbar
        setSupportActionBar(binding.toolbar)
        val title = SpannableString("Meu Ioritv")
        title.setSpan(StyleSpan(Typeface.BOLD), 0, title.length, 0)
        supportActionBar?.title = title

        // Inicializa o estado dos filtros
        filterStates["ATIVO"] = false
        filterStates["A VENCER"] = false
        filterStates["VENCIDO"] = false
        filterStates["STANDBY"] = false

        // Configuração da SearchView
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilters(newText)
                return true
            }
        })

        setupSwipeToRefresh()
        setupFilterButtons()
        loadClientesFromFirebase()

        // Configuração do FloatingActionButton
        binding.fabMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Configuração da Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Navegação para a Activity de Ranking
                    startActivity(Intent(this, RankingActivity::class.java))
                    true
                }
                R.id.nav_cadastros -> {
                    // Navegação para a MainActivity2 (atual)
                    true
                }
                R.id.nav_financas -> {
                    // Navegação para a MainActivity4
                    startActivity(Intent(this, MainActivity4::class.java))
                    true
                }
                else -> false
            }
        }

        // Configura o WorkManager para executar a tarefa de atualização diariamente
        configurarWorkManager()

        // Manter a seleção do menu atual
        binding.bottomNavigation.selectedItemId = R.id.nav_cadastros
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadClientesFromFirebase()
        }
    }

    private fun setupFilterButtons() {
        binding.buttonAtivo.setOnClickListener { toggleFilter("ATIVO", binding.buttonAtivo) }
        binding.buttonAVencer.setOnClickListener { toggleFilter("A VENCER", binding.buttonAVencer) }
        binding.buttonVencido.setOnClickListener { toggleFilter("VENCIDO", binding.buttonVencido) }
        binding.buttonStandby.setOnClickListener { toggleFilter("STANDBY", binding.buttonStandby) }
    }

    private fun loadClientesFromFirebase() {
        binding.swipeRefreshLayout.isRefreshing = true

        db.collection("clientes")
            .orderBy("NOME", Query.Direction.ASCENDING) // Ordena pelo campo 'NOME' em ordem crescente
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Erro ao escutar clientes: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.swipeRefreshLayout.isRefreshing = false
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    clienteList = snapshots.documents
                    setupRecyclerView()
                    applyFilters(binding.searchView.query.toString())
                }
                binding.swipeRefreshLayout.isRefreshing = false
            }
    }

    private fun handleResult(result: QuerySnapshot) {
        clienteList = result.documents // Obter documentos da coleção
        setupRecyclerView()  // Atualiza o RecyclerView
        applyFilters(binding.searchView.query.toString())  // Aplicar os filtros, se houver
    }

    private fun applyFilters(query: String?) {
        var filteredList = clienteList
        val activeFilters = filterStates.filter { it.value }.keys
        if (activeFilters.isNotEmpty()) {
            filteredList = filteredList.filter { cliente ->
                activeFilters.contains(cliente.getString("SITUACAO"))
            }
        }
        query?.let { text ->
            filteredList = filteredList.filter { cliente ->
                cliente.getString("NOME")?.contains(text, ignoreCase = true) == true ||
                        cliente.getString("USUARIO")?.contains(text, ignoreCase = true) == true ||
                        cliente.getString("WHATSAPP")?.contains(text, ignoreCase = true) == true
            }
        }
        updateRecyclerView(filteredList)
    }

    private fun toggleFilter(filter: String, button: View) {
        val isSelected = filterStates[filter] == true
        filterStates[filter] = !isSelected
        button.isSelected = !isSelected
        applyFilters(binding.searchView.query.toString())
    }

    private fun clearFilters() {
        filterStates.keys.forEach { filterStates[it] = false }
        binding.buttonAtivo.isSelected = false
        binding.buttonAVencer.isSelected = false
        binding.buttonVencido.isSelected = false
        binding.buttonStandby.isSelected = false
        applyFilters(binding.searchView.query.toString())
    }

    private fun updateRecyclerView(filteredList: List<DocumentSnapshot>) {
        // Converter List para MutableList se necessário
        val adapter = ClienteAdapter(filteredList.toMutableList()) { cliente ->
            showDeleteConfirmationDialog(cliente)
        }
        binding.recyclerViewClientes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewClientes.adapter = adapter
    }

    private fun showDeleteConfirmationDialog(cliente: DocumentSnapshot) {
        // Criação do diálogo de confirmação
        val dialog = AlertDialog.Builder(this)
            .setTitle("Confirmação de Exclusão")
            .setMessage("Você tem certeza de que deseja excluir este cliente?")
            .setPositiveButton("Sim") { dialog, _ ->
                deleteCliente(cliente)
                dialog.dismiss()
            }
            .setNegativeButton("Não") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        
        // DialogUtils.styleAlertDialogButtons(dialog, this)
        dialog.show()
    }

    private fun deleteCliente(cliente: DocumentSnapshot) {
        val clienteId = cliente.id
        db.collection("clientes").document(clienteId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Cliente excluído com sucesso", Toast.LENGTH_SHORT).show()
                loadClientesFromFirebase() // Recarregar a lista após exclusão
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Falha ao excluir cliente: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewClientes.layoutManager = LinearLayoutManager(this)

        // Converte o clienteList para MutableList
        binding.recyclerViewClientes.adapter = ClienteAdapter(clienteList.toMutableList()) { cliente ->
            showDeleteConfirmationDialog(cliente)
        }
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
        val anchor = binding.toolbar.findViewById<View>(R.id.menu_theme) ?: binding.toolbar
        
        val popupMenu = PopupMenu(this, anchor, android.view.Gravity.END)
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
        val sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
