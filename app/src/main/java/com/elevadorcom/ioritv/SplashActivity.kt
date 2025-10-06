package com.elevadorcom.ioritv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.VideoView
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.util.Log

class SplashActivity : Activity() {

    companion object {
        private const val TAG = "SplashActivity"
        private const val FALLBACK_DELAY = 5000L // 5 segundos se o vídeo falhar
    }

    private lateinit var videoView: VideoView
    private lateinit var fallbackLayout: LinearLayout
    private var videoLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Criar layout com vídeo e fallback
        createVideoLayout()
        
        // Configurar vídeo
        setupVideo()
        
        // Configurar fallback com delay
        setupFallback()
    }
    
    private fun createVideoLayout() {
        // Layout principal
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(getColor(android.R.color.black))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // VideoView
        videoView = VideoView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // Layout de fallback (caso o vídeo não carregue)
        fallbackLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(getColor(android.R.color.black))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            
            // Logo
            val logoImageView = ImageView(this@SplashActivity).apply {
                setImageResource(R.drawable.ic_logo)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 50)
                }
            }
            
            // Texto do app
            val appNameText = TextView(this@SplashActivity).apply {
                text = "IORI.TV"
                textSize = 32f
                setTextColor(getColor(android.R.color.white))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            
            // Texto de carregamento
            val loadingText = TextView(this@SplashActivity).apply {
                text = "Carregando..."
                textSize = 16f
                setTextColor(getColor(android.R.color.darker_gray))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 20, 0, 0)
                }
            }
            
            addView(logoImageView)
            addView(appNameText)
            addView(loadingText)
        }
        
        // Adicionar views ao layout principal
        mainLayout.addView(videoView)
        mainLayout.addView(fallbackLayout)
        
        setContentView(mainLayout)
    }
    
    private fun setupVideo() {
        try {
            // Configurar listener para quando o vídeo estiver pronto
            videoView.setOnPreparedListener { mediaPlayer ->
                Log.d(TAG, "Vídeo preparado com sucesso")
                videoLoaded = true
                videoView.start()
                
                // Garantir que o vídeo termine e vá para a próxima tela
                mediaPlayer.setOnCompletionListener {
                    Log.d(TAG, "Vídeo finalizado")
                    goToLoginActivity()
                }
            }
            
            // Configurar listener para erros de vídeo
            videoView.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "Erro no vídeo: what=$what, extra=$extra")
                showFallback()
                true // Indica que o erro foi tratado
            }
            
            // Tentar carregar o vídeo
            loadVideo()
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao configurar vídeo", e)
            showFallback()
        }
    }
    
    private fun loadVideo() {
        try {
            // Verificar se o arquivo de vídeo existe
            val videoPath = "android.resource://${packageName}/${R.raw.splash}"
            val uri = Uri.parse(videoPath)
            
            Log.d(TAG, "Carregando vídeo: $videoPath")
            videoView.setVideoURI(uri)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar vídeo", e)
            showFallback()
        }
    }
    
    private fun setupFallback() {
        // Se após 3 segundos o vídeo não carregou, mostrar fallback
        Handler(Looper.getMainLooper()).postDelayed({
            if (!videoLoaded) {
                Log.w(TAG, "Timeout - mostrando fallback")
                showFallback()
            }
        }, 3000)
    }
    
    private fun showFallback() {
        Log.d(TAG, "Mostrando layout de fallback")
        videoView.visibility = View.GONE
        fallbackLayout.visibility = View.VISIBLE
        
        // Ir para LoginActivity após delay
        Handler(Looper.getMainLooper()).postDelayed({
            goToLoginActivity()
        }, FALLBACK_DELAY)
    }
    
    private fun goToLoginActivity() {
        Log.d(TAG, "Navegando para LoginActivity")
        try {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao navegar para LoginActivity", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Limpar recursos do vídeo
        try {
            if (::videoView.isInitialized) {
                videoView.stopPlayback()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao limpar vídeo", e)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Pausar vídeo se estiver rodando
        try {
            if (::videoView.isInitialized && videoView.isPlaying) {
                videoView.pause()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao pausar vídeo", e)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Retomar vídeo se estava rodando
        try {
            if (::videoView.isInitialized && videoLoaded && !videoView.isPlaying) {
                videoView.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao retomar vídeo", e)
        }
    }
}