package com.elevadorcom.ioritv

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.elevadorcom.ioritv.databinding.ActivityDespesaBinding
import com.elevadorcom.ioritv.utils.ThemeUtils
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DespesaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDespesaBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica o tema apropriado usando ThemeUtils
        ThemeUtils.applyTheme(this)
        
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityDespesaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Adicionar Despesa"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupViews() {
        // Pré-preencher data atual
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        binding.inputData.setText(currentDate)

        binding.buttonSalvar.setOnClickListener {
            salvarDespesa()
        }
    }

    private fun salvarDespesa() {
        val data = binding.inputData.text.toString()
        val descricao = binding.inputDespesa.text.toString()
        val valorStr = binding.inputValor.text.toString()

        if (data.isEmpty() || descricao.isEmpty() || valorStr.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        val valor = valorStr.toDoubleOrNull()
        if (valor == null || valor <= 0) {
            Toast.makeText(this, "Por favor, insira um valor válido", Toast.LENGTH_SHORT).show()
            return
        }

        val despesaData = hashMapOf(
            "data" to data,
            "descricao" to descricao,
            "valor" to valor,
            "dataTimestamp" to System.currentTimeMillis()
        )

        db.collection("despesas")
            .add(despesaData)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Despesa adicionada com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao salvar despesa: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
