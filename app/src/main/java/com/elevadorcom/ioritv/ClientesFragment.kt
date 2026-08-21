package com.elevadorcom.ioritv

import ClienteAdapter
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.elevadorcom.ioritv.databinding.FragmentClientesBinding
import com.elevadorcom.ioritv.utils.SituacaoUtil
import com.elevadorcom.ioritv.utils.ThemeUtils
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

/**
 * Aba Cadastros — conteúdo migrado da MainActivity2 (Fase 3).
 * Preserva: lista em tempo real, busca, filtros por situação, swipe-to-refresh e exclusão.
 */
class ClientesFragment : Fragment() {

    private var _binding: FragmentClientesBinding? = null
    private val binding get() = _binding!!

    private var clienteList: List<DocumentSnapshot> = emptyList()
    private val db = FirebaseFirestore.getInstance()
    private val filterStates = mutableMapOf<String, Boolean>()
    private var snapshotListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa o estado dos filtros
        filterStates["ATIVO"] = false
        filterStates["A VENCER"] = false
        filterStates["VENCIDO"] = false
        filterStates["STANDBY"] = false

        // Configuração da barra de pesquisa (EditText)
        binding.searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters(s?.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        setupSwipeToRefresh()
        setupFilterButtons()
        applyGlassThemeFixes()
        // Filtro inicial vindo do cartão "Próximo Passo" da Home (one-shot via SharedPreferences)
        applyInitialFilter()
        loadClientesFromFirebase()

        // Configuração do FloatingActionButton — abre o formulário de novo cliente (modo criar)
        binding.fabMenu.setOnClickListener {
            findNavController().navigate(R.id.nav_cliente_form)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snapshotListener?.remove()
        snapshotListener = null
        _binding = null
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

    /**
     * Aplica cores específicas do tema Glass aos botões de filtro e SearchView.
     * No Glass, as superfícies são translúcidas — os botões e busca precisam
     * de fundos semi-transparentes e texto escuro para contraste adequado.
     */
    private var isGlassTheme = false

    /**
     * Aplica cores específicas do tema Glass aos botões de filtro e SearchView.
     * No Glass, as superfícies são translúcidas — os botões e busca precisam
     * de fundos semi-transparentes e texto escuro para contraste adequado.
     */
    private fun applyGlassThemeFixes() {
        isGlassTheme = ThemeUtils.getSavedThemeMode(requireContext()) == ThemeUtils.MODE_GLASS
        if (!isGlassTheme) return

        // SearchView: fundo semi-opaco, texto e hint escuros (sem borda)
        val searchBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#B3FFFFFF"))  // branco 70% - suficiente para legibilidade
            cornerRadius = 48f
        }
        binding.searchCard.setCardBackgroundColor(Color.parseColor("#B3FFFFFF"))
        binding.searchEditText.setTextColor(Color.parseColor("#1C1B33"))
        binding.searchEditText.setHintTextColor(Color.parseColor("#555770"))

        // Aplica estado visual inicial dos botões de filtro
        updateGlassButtonState(binding.buttonAtivo, filterStates["ATIVO"] == true)
        updateGlassButtonState(binding.buttonAVencer, filterStates["A VENCER"] == true)
        updateGlassButtonState(binding.buttonVencido, filterStates["VENCIDO"] == true)
        updateGlassButtonState(binding.buttonStandby, filterStates["STANDBY"] == true)
    }

    /**
     * Atualiza o visual de um botão de filtro no tema Glass.
     */
    private fun updateGlassButtonState(btn: com.google.android.material.button.MaterialButton, isActive: Boolean) {
        if (!isGlassTheme) return
        // Remove o backgroundTint do XML para que nosso GradientDrawable tenha efeito
        btn.backgroundTintList = null
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(if (isActive) "#CC5B6EE1" else "#CCFFFFFF"))
            cornerRadius = 40f
        }
        btn.background = bg
        btn.setTextColor(Color.parseColor("#1C1B33"))
    }





    private fun loadClientesFromFirebase() {
        binding.swipeRefreshLayout.isRefreshing = true

        snapshotListener?.remove()
        snapshotListener = db.collection("clientes")
            .orderBy("NOME", Query.Direction.ASCENDING) // Ordena pelo campo 'NOME' em ordem crescente
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Erro ao escutar clientes: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.swipeRefreshLayout.isRefreshing = false
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    clienteList = snapshots.documents
                    setupRecyclerView()
                    applyFilters(binding.searchEditText.text.toString())
                }
                binding.swipeRefreshLayout.isRefreshing = false
            }
    }

    /**
     * Ativa o filtro vindo do cartão "Próximo Passo" da Home. O filtro chega via
     * SharedPreferences (chave one-shot "pendingFilter") em vez de argumento de navegação:
     * passar argumentos a um destino de aba quebra o restoreState da bottom nav
     * (o tap em Home passava a restaurar o estado errado).
     */
    private fun applyInitialFilter() {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val filtro = prefs.getString("pendingFilter", null) ?: return
        prefs.edit().remove("pendingFilter").apply()
        val button = when (filtro) {
            "ATIVO" -> binding.buttonAtivo
            "A VENCER" -> binding.buttonAVencer
            "VENCIDO" -> binding.buttonVencido
            "STANDBY" -> binding.buttonStandby
            else -> return
        }
        filterStates[filtro] = true
        button.isSelected = true
        applyFilters(binding.searchEditText.text.toString())
    }

    private fun applyFilters(query: String?) {
        var filteredList = clienteList
        val activeFilters = filterStates.filter { it.value }.keys
        if (activeFilters.isNotEmpty()) {
            filteredList = filteredList.filter { cliente ->
                // Situação calculada pelo TERMINO (fonte canônica) com fallback para o valor gravado.
                // O adapter exibe a calculada — o filtro deve acompanhar para não divergir.
                val situacao = cliente.getTimestamp("TERMINO")?.toDate()
                    ?.let { SituacaoUtil.calcularSituacao(it) }
                    ?: cliente.getString("SITUACAO") ?: ""
                activeFilters.contains(situacao)
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
        if (isGlassTheme && button is com.google.android.material.button.MaterialButton) {
            updateGlassButtonState(button, !isSelected)
        }
        applyFilters(binding.searchEditText.text.toString())
    }

    private fun updateRecyclerView(filteredList: List<DocumentSnapshot>) {
        val adapter = ClienteAdapter(
            filteredList.toMutableList(),
            onDeleteClick = { cliente -> showDeleteConfirmationDialog(cliente) },
            onEditClick = { cliente -> navigateToEditClient(cliente) },
            isGlassTheme = isGlassTheme
        )
        binding.recyclerViewClientes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewClientes.adapter = adapter
    }

    private fun setupRecyclerView() {
        binding.recyclerViewClientes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewClientes.adapter = ClienteAdapter(
            clienteList.toMutableList(),
            onDeleteClick = { cliente -> showDeleteConfirmationDialog(cliente) },
            onEditClick = { cliente -> navigateToEditClient(cliente) },
            isGlassTheme = isGlassTheme
        )
    }

    /**
     * Navega para o formulário em modo edição (Fase 5 — substitui a EditCadastroActivity).
     */
    private fun navigateToEditClient(cliente: DocumentSnapshot) {
        findNavController().navigate(
            R.id.nav_cliente_form,
            bundleOf("clienteId" to cliente.id)
        )
    }

    private fun showDeleteConfirmationDialog(cliente: DocumentSnapshot) {
        // Criação do diálogo de confirmação
        val dialog = AlertDialog.Builder(requireContext())
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

        dialog.show()
    }

    private fun deleteCliente(cliente: DocumentSnapshot) {
        val clienteId = cliente.id
        db.collection("clientes").document(clienteId)
            .delete()
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Cliente excluído com sucesso", Toast.LENGTH_SHORT).show()
                loadClientesFromFirebase() // Recarregar a lista após exclusão
            }
            .addOnFailureListener { exception ->
                if (_binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Falha ao excluir cliente: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
