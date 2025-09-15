package com.elevadorcom.ioritv

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.elevadorcom.ioritv.databinding.ItemFinanceiroBinding

data class FinanceiroItem(val titulo: String, val valor: String)

class FinanceiroAdapter(private var items: List<FinanceiroItem>) :
    RecyclerView.Adapter<FinanceiroAdapter.FinanceiroViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FinanceiroViewHolder {
        val binding = ItemFinanceiroBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FinanceiroViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FinanceiroViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<FinanceiroItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class FinanceiroViewHolder(private val binding: ItemFinanceiroBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FinanceiroItem) {
            binding.textViewTitulo.text = item.titulo
            binding.textViewValor.text = item.valor
        }
    }
}
