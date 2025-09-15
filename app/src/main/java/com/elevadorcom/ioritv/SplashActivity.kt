package com.elevadorcom.ioritv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Esconde a barra de ação

        val videoView = VideoView(this)
        setContentView(videoView) // Define o VideoView como a tela inteira

        // Obtém o caminho do vídeo dentro da pasta 'raw'
        val videoPath = "android.resource://${packageName}/${R.raw.splash}"
        val uri = Uri.parse(videoPath)

        videoView.setVideoURI(uri)
        videoView.setOnCompletionListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        videoView.start() // Inicia a reprodução do vídeo
    }
}
