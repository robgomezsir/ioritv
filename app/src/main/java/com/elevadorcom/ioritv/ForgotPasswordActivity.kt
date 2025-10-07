package com.elevadorcom.ioritv

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.elevadorcom.ioritv.databinding.ActivityForgotPasswordBinding
import com.elevadorcom.ioritv.utils.ThemeUtils
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica o tema apropriado usando ThemeUtils
        ThemeUtils.applyTheme(this)

        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.resetPasswordButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                binding.emailEditText.error = "Email é obrigatório"
                binding.emailEditText.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailEditText.error = "Insira um email válido"
                binding.emailEditText.requestFocus()
                return@setOnClickListener
            }

            resetPassword(email)
        }
    }

    private fun resetPassword(email: String) {
        binding.resetPasswordButton.isEnabled = false
        binding.resetPasswordButton.text = "Enviando..."

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.resetPasswordButton.isEnabled = true
                binding.resetPasswordButton.text = "Redefinir Senha"

                if (task.isSuccessful) {
                    Toast.makeText(this, "Email de redefinição de senha enviado!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Erro ao enviar email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
