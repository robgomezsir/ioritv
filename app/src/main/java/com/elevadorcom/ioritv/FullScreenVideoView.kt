package com.elevadorcom.ioritv

import android.content.Context
import android.util.AttributeSet
import android.widget.VideoView

class FullScreenVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VideoView(context, attrs, defStyleAttr) {

    private var videoWidth = 0
    private var videoHeight = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        
        if (videoWidth > 0 && videoHeight > 0) {
            // Calcular as escalas para preencher toda a tela
            val videoAspectRatio = videoWidth.toFloat() / videoHeight.toFloat()
            val screenAspectRatio = width.toFloat() / height.toFloat()
            
            val scaledWidth: Int
            val scaledHeight: Int
            
            if (videoAspectRatio > screenAspectRatio) {
                // Vídeo é mais largo que a tela - ajustar pela altura
                scaledHeight = height
                scaledWidth = (height * videoAspectRatio).toInt()
            } else {
                // Vídeo é mais alto que a tela - ajustar pela largura
                scaledWidth = width
                scaledHeight = (width / videoAspectRatio).toInt()
            }
            
            setMeasuredDimension(scaledWidth, scaledHeight)
        } else {
            // Fallback: usar dimensões da tela
            setMeasuredDimension(width, height)
        }
    }

    fun setVideoSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
        requestLayout()
    }
}
