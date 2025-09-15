import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.animation.addListener
import androidx.recyclerview.widget.RecyclerView
import com.elevadorcom.ioritv.EditCadastroActivity
import com.elevadorcom.ioritv.R
import com.elevadorcom.ioritv.utils.SituacaoUtil
import com.elevadorcom.ioritv.utils.SituacaoConstants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ClienteAdapter(
    private val clientes: List<DocumentSnapshot>,
    private val onDeleteClick: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<ClienteAdapter.ClienteViewHolder>() {

    private val db = FirebaseFirestore.getInstance() // Instância do Firestore

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cliente, parent, false)
        return ClienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        val cliente = clientes[position]
        holder.bind(cliente)
    }

    override fun getItemCount(): Int = clientes.size

    inner class ClienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textNome: TextView = itemView.findViewById(R.id.textViewNome)
        private val textSituacao: TextView = itemView.findViewById(R.id.textViewSituacao)
        private val textUsuario: TextView = itemView.findViewById(R.id.textViewUsuario)
        private val textWhatsapp: TextView = itemView.findViewById(R.id.textViewWhatsApp)
        private val textSenha: TextView = itemView.findViewById(R.id.textViewSenha)
        private val textModelo: TextView = itemView.findViewById(R.id.textViewModelo)
        private val textInicio: TextView = itemView.findViewById(R.id.textViewInicio)
        private val textTermino: TextView = itemView.findViewById(R.id.textViewTermino)
        private val textCreditos: TextView = itemView.findViewById(R.id.textViewCreditos)
        private val textVencimento: TextView = itemView.findViewById(R.id.textViewVencimento)
        private val textMAC: TextView = itemView.findViewById(R.id.textViewMac)
        private val textOTP: TextView = itemView.findViewById(R.id.textViewOtp)
        private val textDevice: TextView = itemView.findViewById(R.id.textViewDevice)
        private val textValor: TextView = itemView.findViewById(R.id.textViewValor)
        private val textDesconto: TextView = itemView.findViewById(R.id.textViewDesconto)
        private val textCusto: TextView = itemView.findViewById(R.id.textViewCusto)
        private val textServidor: TextView = itemView.findViewById(R.id.textViewServidor)
        private val expandableSection: View = itemView.findViewById(R.id.expandedLayout)
        private val statusIcon: ImageView = itemView.findViewById(R.id.statusIcon) // ImageView para o ícone de status
        private val btnLiberarTV: Button = itemView.findViewById(R.id.atualizarButton) // Botão Liberar TV
        private val btnDelete: Button = itemView.findViewById(R.id.btnDeletar) // Botão Delete

        private var isExpanded = false

        fun bind(cliente: DocumentSnapshot) {
            // Preencher os campos com rótulos
            textNome.text = "Nome: ${cliente.getString("NOME") ?: ""}"
            textSituacao.text = "Situação: ${cliente.getString("SITUACAO") ?: ""}"
            textUsuario.text = "Usuário: ${cliente.getString("USUARIO") ?: ""}"
            textWhatsapp.text = "WhatsApp: ${cliente.getString("WHATSAPP") ?: ""}"
            textSenha.text = "Senha: ${cliente.getString("SENHA") ?: ""}"
            textModelo.text = "Modelo: ${cliente.getString("MODELO") ?: ""}"

            // Convertendo o timestamp para data legível
            val inicioTimestamp = cliente.getTimestamp("INICIO")
            textInicio.text = "Início: ${inicioTimestamp?.toDate()?.formatToDate() ?: ""}"

            val terminoTimestamp = cliente.getTimestamp("TERMINO")
            textTermino.text = "Término: ${terminoTimestamp?.toDate()?.formatToDate() ?: ""}"

            // Campos numéricos
            val creditos = cliente.getLong("CREDITOS") ?: 0
            textCreditos.text = "Créditos: $creditos"

            // Atualiza o campo VENCIMENTO com base na lógica de datas
            val vencimentoText = calculateVencimentoText(inicioTimestamp, terminoTimestamp)
            textVencimento.text = "Vencimento: $vencimentoText"

            textMAC.text = "MAC: ${cliente.getString("MAC") ?: ""}"
            textOTP.text = "OTP: ${cliente.getString("OTP") ?: ""}"
            textDevice.text = "Device: ${cliente.getString("DEVICE") ?: ""}"

            // Formatar campos de valor em formato monetário
            val valor = cliente.getDouble("VALOR") ?: 0.0
            textValor.text = "Valor: ${formatCurrency(valor)}"

            val desconto = cliente.getDouble("DESCONTO") ?: 0.0
            textDesconto.text = "Desconto: ${formatCurrency(desconto)}"

            val custo = cliente.getDouble("CUSTO") ?: 0.0
            textCusto.text = "Custo: ${formatCurrency(custo)}"

            textServidor.text = "Servidor: ${cliente.getString("SERVIDOR") ?: ""}"

            // Atualizar ícone de status com base na situação
            val situacao = terminoTimestamp?.toDate()?.let { SituacaoUtil.calcularSituacao(it) } ?: "N/A"
            textSituacao.text = "Situação: $situacao"

            updateStatusIcon(situacao)

            // Mostrar ou ocultar o botão "Liberar TV"
            btnLiberarTV.visibility = when (situacao) {
                SituacaoConstants.ATIVO, SituacaoConstants.A_VENCER,
                SituacaoConstants.VENCIDO, SituacaoConstants.STANDBY -> View.VISIBLE
                else -> View.GONE
            }

            // Listener para o clique no botão "Liberar TV"
            btnLiberarTV.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, EditCadastroActivity::class.java)
                intent.putExtra("cadastroId", cliente.id) // Passar o ID do cliente para a próxima Activity
                context.startActivity(intent)
            }

            // Listener para o clique no botão "Delete"
            btnDelete.setOnClickListener {
                showDeleteConfirmationDialog(cliente)
            }

            // Listener para o clique no card
            itemView.setOnClickListener {
                toggleExpansion()
            }

            // Medida inicial da seção expansível
            expandableSection.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // Função auxiliar para zerar a hora, minuto, segundo e milissegundo das datas
        private fun clearTime(date: Date): Date {
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.time
        }

        // Função para calcular a diferença de dias e retornar o texto apropriado
        private fun calculateVencimentoText(inicioTimestamp: com.google.firebase.Timestamp?, terminoTimestamp: com.google.firebase.Timestamp?): String {
            if (terminoTimestamp == null) return "Data de término não disponível"

            val terminoDate = clearTime(terminoTimestamp.toDate())
            val hoje = clearTime(Date())

            val diasRestantes = calculateDaysDifference(hoje, terminoDate)

            return when {
                diasRestantes > 3 -> "Faltam $diasRestantes dias"
                diasRestantes in 1..2 -> "Ainda falta(m) $diasRestantes dia(s)"
                diasRestantes == 0 -> "Vence hoje"
                diasRestantes < 0 -> "Já são ${-diasRestantes} dias vencidos"
                else -> "Faltam $diasRestantes dias"
            }
        }

        // Função para calcular a diferença de dias entre duas datas
        private fun calculateDaysDifference(hoje: Date, termino: Date): Int {
            val diffInMillis = termino.time - hoje.time
            return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        }

        // Atualiza o campo VENCIMENTO com base na lógica de datas
        @SuppressLint("SetTextI18n")
        private fun updateVencimento(inicioTimestamp: Timestamp?, terminoTimestamp: Timestamp?) {
            val vencimentoText = calculateVencimentoText(inicioTimestamp, terminoTimestamp)
            textVencimento.text = "Vencimento: $vencimentoText"
        }

        private fun toggleExpansion() {
            isExpanded = !isExpanded

            if (isExpanded) {
                // Se for expandir, precisamos medir o tamanho da seção expansível
                expandableSection.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                val targetHeight = expandableSection.measuredHeight

                // Começa com altura zero (colapsado) e anima até a altura medida (expandido)
                expandableSection.layoutParams.height = 0
                expandableSection.visibility = View.VISIBLE

                val animator = ValueAnimator.ofInt(0, targetHeight)
                animator.duration = 400
                animator.interpolator = AccelerateDecelerateInterpolator()

                animator.addUpdateListener { valueAnimator ->
                    val animatedValue = valueAnimator.animatedValue as Int
                    expandableSection.layoutParams.height = animatedValue
                    expandableSection.requestLayout()
                }

                animator.start()
            } else {
                // Se for recolher, pega a altura atual e anima até 0 (colapsado)
                val initialHeight = expandableSection.measuredHeight
                val animator = ValueAnimator.ofInt(initialHeight, 0)
                animator.duration = 400
                animator.interpolator = AccelerateDecelerateInterpolator()

                animator.addUpdateListener { valueAnimator ->
                    val animatedValue = valueAnimator.animatedValue as Int
                    expandableSection.layoutParams.height = animatedValue
                    expandableSection.requestLayout()
                }

                animator.addListener(onEnd = {
                    expandableSection.visibility = View.GONE
                })

                animator.start()
            }
        }

        private fun updateStatusIcon(situacao: String) {
            val statusIconResId = when (situacao) {
                SituacaoConstants.ATIVO -> R.drawable.ic_ativo
                SituacaoConstants.A_VENCER -> R.drawable.ic_a_vencer
                SituacaoConstants.VENCIDO -> R.drawable.ic_vencido
                SituacaoConstants.STANDBY -> R.drawable.ic_standby
                else -> R.drawable.logo
            }
            statusIcon.setImageResource(statusIconResId)
        }

        private fun showDeleteConfirmationDialog(cliente: DocumentSnapshot) {
            val context = itemView.context
            AlertDialog.Builder(context)
                .setTitle("Confirmar Exclusão")
                .setMessage("Você tem certeza realmente que deseja excluir este cliente?")
                .setPositiveButton("Sim") { _, _ ->
                    onDeleteClick(cliente)
                }
                .setNegativeButton("Não", null)
                .show()
        }

        private fun formatCurrency(value: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            return format.format(value)
        }

        private fun Date.formatToDate(): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            return sdf.format(this)
        }
    }
}