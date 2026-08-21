package com.elevadorcom.ioritv

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import java.util.concurrent.Executor

class BiometricPromptFragment : DialogFragment() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Executor é necessário para rodar operações de autenticação
        executor = ContextCompat.getMainExecutor(requireContext())

        // Configurações da BiometricPrompt para lidar com callbacks de autenticação
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(requireContext(), "Erro de autenticação: $errString", Toast.LENGTH_SHORT).show()
                dismiss() // Fecha o popup quando ocorrer erro
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(requireContext(), "Autenticação bem-sucedida", Toast.LENGTH_SHORT).show()

                // Após autenticação bem-sucedida, navega para a tela principal (HomeActivity)
                val intent = Intent(requireContext(), HomeActivity::class.java)
                startActivity(intent)
                activity?.finish() // Fecha a atividade atual
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(requireContext(), "Falha de autenticação", Toast.LENGTH_SHORT).show()
            }
        })

        // Configurar o diálogo de autenticação
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticação por Biometria")
            .setSubtitle("Use sua digital ou rosto para autenticar")
            .setNegativeButtonText("Cancelar")
            .build()

        // Exibe automaticamente o prompt de autenticação
        showBiometricPrompt()
    }

    // Função que exibe o prompt de autenticação biométrica
    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(requireContext())
        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Toast.makeText(requireContext(), "Nenhum hardware biométrico disponível", Toast.LENGTH_SHORT).show()
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Toast.makeText(requireContext(), "Hardware biométrico indisponível", Toast.LENGTH_SHORT).show()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                Toast.makeText(requireContext(), "Nenhum dado biométrico cadastrado", Toast.LENGTH_SHORT).show()
        }
    }
}
