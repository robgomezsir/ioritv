package com.elevadorcom.ioritv

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
// import com.elevadorcom.ioritv.utils.DialogUtils
import com.elevadorcom.ioritv.utils.ThemeUtils

class SettingsActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var storageRef: StorageReference
    private lateinit var userNameTextView: TextView
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica o tema apropriado usando ThemeUtils
        ThemeUtils.applyTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        profileImage = findViewById(R.id.profileImage)
        userNameTextView = findViewById(R.id.userNameTextView)

        // Exemplo de nome e foto de perfil
        // Inicializa Firebase Storage e Auth
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid
        storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$userId.jpg")

        // Carrega a imagem do Firebase Storage (caso exista)
        loadImageFromFirebase()

        // Verifica se o usuário tem um nome configurado e exibe
        if (user != null) {
            userNameTextView.text = user.displayName ?: "Usuário sem nome"
        }

        // Definir um clique na imagem para permitir upload
        profileImage.setOnClickListener {
            selectImageFromGallery()
        }

        // Ação para o item Tema
        val themeItem = findViewById<TextView>(R.id.themeOption)
        themeItem.setOnClickListener {
            showThemeDialog()
        }
    }

    // Mostra o AlertDialog para escolher o tema
    private fun showThemeDialog() {
        val themes = arrayOf("Automático", "Claro", "Escuro")
        val currentTheme = ThemeUtils.getCurrentThemeIndex(this)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Escolha o Tema")
            .setSingleChoiceItems(themes, currentTheme) { dialog, which ->
                when (which) {
                    0 -> setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    1 -> setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
                    2 -> setThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        
        // DialogUtils.styleAlertDialogButtons(dialog, this)
        dialog.show()
    }

    // Método para salvar o tema escolhido
    private fun setThemeMode(mode: Int) {
        ThemeUtils.saveThemeMode(this, mode)
        
        // Reinicia a Activity para aplicar o novo tema
        recreate()
    }

    // Carrega a imagem do Firebase Storage
    private fun loadImageFromFirebase() {
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            // Carregar a imagem usando Picasso (ou outra biblioteca de imagem)
            Picasso.get().load(uri).placeholder(R.drawable.profile_placeholder).into(profileImage)
        }.addOnFailureListener {
            // Em caso de erro, manter o placeholder
        }
    }

    // Método para selecionar imagem da galeria
    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // Método para lidar com o resultado da seleção de imagem
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val imageUri: Uri = data.data!!
            uploadImageToFirebase(imageUri)
        }
    }

    // Upload da imagem selecionada para o Firebase Storage
    private fun uploadImageToFirebase(imageUri: Uri) {
        val uploadTask = storageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            // Obter o URL da imagem após upload bem-sucedido
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                // Atualizar a UI com a nova imagem
                Picasso.get().load(uri).placeholder(R.drawable.profile_placeholder).into(profileImage)
            }
        }.addOnFailureListener {
            // Em caso de erro
        }
    }
}
