import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.elevadorcom.ioritv.R
import com.google.firebase.firestore.DocumentSnapshot
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class DespesasAdapter(
    private val context: Context,
    private val onEditClick: (DocumentSnapshot) -> Unit,
    private val onDeleteClick: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<DespesasAdapter.ViewHolder>() {

    private var despesasList: List<DocumentSnapshot> = emptyList()

    fun setDespesas(despesas: List<DocumentSnapshot>) {
        despesasList = despesas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_despesa, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val despesa = despesasList[position]
        holder.bind(despesa)
    }

    override fun getItemCount() = despesasList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewDespesa: TextView = itemView.findViewById(R.id.textViewDespesa)
        private val textViewValor: TextView = itemView.findViewById(R.id.textViewValor)
        private val textViewData: TextView = itemView.findViewById(R.id.textViewData)

        fun bind(despesa: DocumentSnapshot) {
            textViewDespesa.text = despesa.getString("Despesa")
            textViewValor.text = despesa.getDouble("Valor")?.let {
                NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(it)
            } ?: "R$ 0,00"

            // Formatando a data para exibição
            val timestamp = despesa.getTimestamp("Data")
            textViewData.text = timestamp?.let {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.toDate())
            } ?: "Data não disponível"

            itemView.setOnLongClickListener {
                showPopupMenu(it, despesa)
                true
            }
        }

        private fun showPopupMenu(view: View, despesa: DocumentSnapshot) {
            val inflater = LayoutInflater.from(context)
            val popupView = inflater.inflate(R.layout.context_menu, null)
            val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)

            // Configurar o popup menu
            popupWindow.isOutsideTouchable = true
            popupWindow.isFocusable = true

            val actionEdit = popupView.findViewById<TextView>(R.id.action_edit)
            val actionDelete = popupView.findViewById<TextView>(R.id.action_delete)

            actionEdit.setOnClickListener {
                onEditClick(despesa)
                popupWindow.dismiss()
            }
            actionDelete.setOnClickListener {
                onDeleteClick(despesa)
                popupWindow.dismiss()
            }

            // Mostrar o PopupWindow centralizado
            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
        }
    }
}
