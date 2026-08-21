package com.elevadorcom.ioritv

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.elevadorcom.ioritv.databinding.ActivityLoginBinding
import com.elevadorcom.ioritv.utils.SessionManager
import com.elevadorcom.ioritv.utils.ThemeUtils
import com.elevadorcom.ioritv.utils.VersionUtils
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sessionManager: SessionManager
    
    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica o tema apropriado usando ThemeUtils
        ThemeUtils.applyTheme(this)
        
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Skin Glass ativa: backdrop vibrante + barras transparentes
        if (ThemeUtils.isGlassEnabled(this)) {
            binding.root.setBackgroundResource(R.drawable.bg_glass_paint)
            // Barras de sistema transparentes para o aurora brilhar
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }

        auth = FirebaseAuth.getInstance()
        sessionManager = SessionManager.getInstance(this)
        
        // Configura a versão do app no rodapé
        setupVersionInfo()
        
        // Configura o estado do checkbox baseado na preferência salva
        setupKeepLoggedInCheckbox()

        // Verifica se o usuário já está logado
        if (sessionManager.isUserLoggedIn()) {
            // Se já estiver logado, navega para a tela principal
            navigateToMainScreen()
        } else {
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
        var isValid = true
        
        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Email é obrigatório"
            binding.emailEditText.requestFocus()
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Insira um email válido"
            binding.emailEditText.requestFocus()
            isValid = false
        } else {
            binding.emailInputLayout.error = null
        }

        if (password.isEmpty()) {
            binding.passwordInputLayout.error = "Senha é obrigatória"
            binding.passwordEditText.requestFocus()
            isValid = false
        } else if (password.length < 6) {
            binding.passwordInputLayout.error = "A senha deve ter no mínimo 6 caracteres"
            binding.passwordEditText.requestFocus()
            isValid = false
        } else {
            binding.passwordInputLayout.error = null
        }

        return isValid
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
                        // Verifica se o checkbox "Manter-me conectado" está marcado
                        if (binding.keepLoggedInCheckbox.isChecked) {
                            // Salva o estado de login para persistência
                            sessionManager.saveLoginStatus()
                            Log.d(TAG, "Login successful with persistent session for user: ${user.email}")
                        } else {
                            // Não salva a sessão - usuário será deslogado ao fechar o app
                            sessionManager.clearLoginStatus()
                            Log.d(TAG, "Login successful without persistent session for user: ${user.email}")
                        }
                        
                        // Navega para a tela principal
                        navigateToMainScreen()
                    } else if (user != null && !user.isEmailVerified) {
                        Toast.makeText(this, "Verifique seu email para continuar", Toast.LENGTH_SHORT).show()
                        auth.signOut() // Faz logout se email não verificado
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Falha ao fazer login"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Login failed: $errorMessage")
                }
            }
    }

    // Função para exibir o prompt de biometria
    private fun showBiometricPrompt() {
        val biometricFragment = BiometricPromptFragment()
        biometricFragment.show(supportFragmentManager, "BiometricPromptFragment")
    }

    // Função para navegar para a tela principal
    private fun navigateToMainScreen() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
    
    // Função para configurar a versão do app no rodapé
    private fun setupVersionInfo() {
        try {
            binding.textView2.text = VersionUtils.getFooterText(this)
        } catch (e: Exception) {
            // Mantém o texto padrão em caso de erro
            e.printStackTrace()
        }
    }
    
    // Função para configurar o checkbox "Manter-me conectado"
    private fun setupKeepLoggedInCheckbox() {
        // Restaura a preferência salva (padrão: true)
        val keepLoggedIn = sessionManager.getKeepLoggedInPreference()
        binding.keepLoggedInCheckbox.isChecked = keepLoggedIn
        
        // Salva a preferência quando o checkbox é alterado
        binding.keepLoggedInCheckbox.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.setKeepLoggedInPreference(isChecked)
            Log.d(TAG, "Keep logged in preference set to: $isChecked")
        }
    }
}
