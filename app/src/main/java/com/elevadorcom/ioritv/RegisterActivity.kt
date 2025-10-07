package com.elevadorcom.ioritv

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.elevadorcom.ioritv.databinding.ActivityRegisterBinding
import com.elevadorcom.ioritv.utils.ThemeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica o tema apropriado usando ThemeUtils
        ThemeUtils.applyTheme(this)

        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.registerButton.setOnClickListener {
            val nome = binding.nomeEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            if (validateInput(email, password, confirmPassword)) {
                registerUser(email, password, nome)  // Passando o nome para registro
            }
        }
    }

    private fun validateInput(email: String, password: String, confirmPassword: String): Boolean {
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

        if (password != confirmPassword) {
            binding.confirmPasswordEditText.error = "As senhas não coincidem"
            binding.confirmPasswordEditText.requestFocus()
            return false
        }

        return true
    }

    private fun registerUser(email: String, password: String, nome: String) {
        binding.registerButton.isEnabled = false
        binding.registerButton.text = "Registrando..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.registerButton.isEnabled = true
                binding.registerButton.text = "Registrar"

                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Atualizando o nome do usuário no perfil
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(nome)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("RegisterActivity", "Nome de usuário atualizado com sucesso.")
                        }
                    }

                    // Salvando o nome no Firestore
                    val userId = user?.uid
                    val db = FirebaseFirestore.getInstance()
                    val userData = hashMapOf(
                        "name" to nome,
                        "email" to email
                    )

                    if (userId != null) {
                        db.collection("users").document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                Log.d("RegisterActivity", "Nome salvo no Firestore.")
                            }
                            .addOnFailureListener { e ->
                                Log.w("RegisterActivity", "Erro ao salvar nome no Firestore", e)
                            }
                    }

                    Toast.makeText(this, "Usuário registrado com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Erro ao registrar usuário: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
