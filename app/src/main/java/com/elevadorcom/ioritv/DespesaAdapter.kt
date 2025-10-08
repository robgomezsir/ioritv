package com.elevadorcom.ioritv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.*

class DespesaAdapter(
    private var despesas: List<DespesaItem>,
    private val onItemClick: (DespesaItem) -> Unit
) : RecyclerView.Adapter<DespesaAdapter.DespesaViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    class DespesaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView as MaterialCardView
        val textData: TextView = itemView.findViewById(R.id.textViewData)
        val textDescricao: TextView = itemView.findViewById(R.id.textViewDespesa)
        val textValor: TextView = itemView.findViewById(R.id.textViewValor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DespesaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_despesa, parent, false)
        return DespesaViewHolder(view)
    }

    override fun onBindViewHolder(holder: DespesaViewHolder, position: Int) {
        val despesa = despesas[position]
        
        holder.textData.text = despesa.data
        holder.textDescricao.text = despesa.descricao
        holder.textValor.text = currencyFormat.format(despesa.valor)
        
        // Configurar clique no card
        holder.cardView.setOnClickListener {
            onItemClick(despesa)
        }
        
        // Adicionar feedback visual de clique
        holder.cardView.isClickable = true
        holder.cardView.isFocusable = true
    }

    override fun getItemCount(): Int = despesas.size

    fun updateDespesas(newDespesas: List<DespesaItem>) {
        despesas = newDespesas
        notifyDataSetChanged()
    }
}

data class DespesaItem(
    val id: String,
    val data: String,
    val descricao: String,
    val valor: Double
)
