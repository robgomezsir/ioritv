package com.elevadorcom.ioritv

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.elevadorcom.ioritv.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE)

        // Verifica se o usuário já está logado
        if (!checkIfLoggedIn()) {
            // Se não estiver logado, exibe o pop-up de biometria
            showBiometricPrompt()
        }

        // Configura o botão de login manual
        val btnLoginManual = findViewById<Button>(R.id.loginButton)
        btnLoginManual.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (validateInput(email, password)) {
                loginUser(email, password)
            }
        }

        // Navegação para a tela de registro
        binding.registerTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Navegação para a tela de recuperação de senha
        binding.forgotPasswordTextView.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    // Função de validação de email e senha
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.emailEditText.error = "Email é obrigatório"
            binding.emailEditText.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Insira um email válido"
            binding.emailEditText.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            binding.passwordEditText.error = "Senha é obrigatória"
            binding.passwordEditText.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.passwordEditText.error = "A senha deve ter no mínimo 6 caracteres"
            binding.passwordEditText.requestFocus()
            return false
        }

        return true
    }

    // Função de login com Firebase Authentication
    private fun loginUser(email: String, password: String) {
        binding.loginButton.isEnabled = false
        binding.loginButton.text = "Entrando..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.loginButton.isEnabled = true
                binding.loginButton.text = "Entrar"

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        saveLoginStatus() // Salva o estado de login
                        startActivity(Intent(this, RankingActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Verifique seu email para continuar", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Falha ao fazer login", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Função para exibir o prompt de biometria
    private fun showBiometricPrompt() {
        val biometricFragment = BiometricPromptFragment()
        biometricFragment.show(supportFragmentManager, "BiometricPromptFragment")
    }

    // Função para salvar o estado de login
    private fun saveLoginStatus() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("is_logged_in", true)
        editor.apply()
    }

    // Função para verificar se o usuário já está logado
    private fun checkIfLoggedIn(): Boolean {
        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)
        if (isLoggedIn && auth.currentUser != null) {
            // Se o usuário já estiver logado, navega diretamente para a RankingActivity
            startActivity(Intent(this, RankingActivity::class.java))
            finish()
            return true
        }
        return false
    }
}
