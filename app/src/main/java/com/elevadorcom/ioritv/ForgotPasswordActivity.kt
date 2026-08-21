package com.elevadorcom.ioritv

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.elevadorcom.ioritv.databinding.ActivityForgotPasswordBinding
import com.elevadorcom.ioritv.utils.ThemeUtils
import com.elevadorcom.ioritv.utils.FirebaseAuthDiagnostics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ActionCodeSettings

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica o tema apropriado usando ThemeUtils
        ThemeUtils.applyTheme(this)

        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Skin Glass: barras de sistema transparentes
        if (ThemeUtils.isGlassEnabled(this)) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }

        auth = FirebaseAuth.getInstance()

        // Executar diagnóstico para debug
        FirebaseAuthDiagnostics.runDiagnostics(this)

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

        // Configurações avançadas para o email de recuperação
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://ioritv-70318.firebaseapp.com/reset-password") // URL customizada do seu projeto
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                "com.elevadorcom.ioritv",
                true, // installIfNotAvailable
                "1" // minimumVersion
            )
            .build()

        // Primeiro tenta com configurações customizadas
        auth.sendPasswordResetEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    binding.resetPasswordButton.isEnabled = true
                    binding.resetPasswordButton.text = "Redefinir Senha"
                    Toast.makeText(this, "Email de redefinição enviado para $email", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    // Se falhar com configurações customizadas, tenta o método simples
                    resetPasswordSimple(email)
                }
            }
            .addOnFailureListener { exception ->
                // Se falhar com configurações customizadas, tenta o método simples
                resetPasswordSimple(email)
            }
    }

    private fun resetPasswordSimple(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.resetPasswordButton.isEnabled = true
                binding.resetPasswordButton.text = "Redefinir Senha"

                if (task.isSuccessful) {
                    Toast.makeText(this, "Email de redefinição enviado para $email", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val error = task.exception
                    val errorMessage = when {
                        error?.message?.contains("user-not-found") == true -> 
                            "Nenhuma conta encontrada com este email"
                        error?.message?.contains("invalid-email") == true -> 
                            "Email inválido"
                        error?.message?.contains("too-many-requests") == true -> 
                            "Muitas tentativas. Tente novamente mais tarde"
                        error?.message?.contains("network-request-failed") == true -> 
                            "Erro de conexão. Verifique sua internet"
                        error?.message?.contains("operation-not-allowed") == true -> 
                            "Operação não permitida. Contate o suporte"
                        else -> 
                            "Erro ao enviar email: ${error?.message ?: "Erro desconhecido"}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    android.util.Log.e("ForgotPassword", "Erro ao enviar email de recuperação: ${error?.message}", error)
                }
            }
            .addOnFailureListener { exception ->
                binding.resetPasswordButton.isEnabled = true
                binding.resetPasswordButton.text = "Redefinir Senha"
                Toast.makeText(this, "Falha na conexão: ${exception.message}", Toast.LENGTH_LONG).show()
                android.util.Log.e("ForgotPassword", "Falha ao enviar email de recuperação", exception)
            }
    }
}
