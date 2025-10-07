package com.elevadorcom.ioritv.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.ProgressBar

object AnimationUtils {

    /**
     * Anima a entrada de um card com efeito de bounce
     */
    fun animateCardEntry(card: MaterialCardView, delay: Long = 0) {
        card.alpha = 0f
        card.scaleX = 0.8f
        card.scaleY = 0.8f
        
        card.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setStartDelay(delay)
            .setInterpolator(BounceInterpolator())
            .start()
    }

    /**
     * Anima múltiplos cards em sequência
     */
    fun animateCardsEnter(cards: List<View>, delayBetweenCards: Long = 100) {
        cards.forEachIndexed { index, card ->
            if (card is MaterialCardView) {
                animateCardEntry(card, (index * delayBetweenCards).toLong())
            } else {
                // Para views que não são MaterialCardView
                card.alpha = 0f
                card.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay((index * delayBetweenCards).toLong())
                    .start()
            }
        }
    }

    /**
     * Anima a transição de loading para conteúdo
     */
    fun transitionFromLoadingToContent(loadingView: View, contentView: View) {
        loadingView.animate()
            .alpha(0f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    loadingView.visibility = View.GONE
                    contentView.visibility = View.VISIBLE
                    contentView.alpha = 0f
                    contentView.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                }
            })
            .start()
    }

    /**
     * Anima um botão quando clicado
     */
    fun animateButtonClick(view: View, onAnimationEnd: () -> Unit = {}) {
        val animator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f)
        val animatorY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f)
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animator, animatorY)
        animatorSet.duration = 150
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onAnimationEnd()
            }
        })
        animatorSet.start()
    }

    /**
     * Anima a rotação de uma view (útil para loading)
     */
    fun rotate(view: View, duration: Long = 1000, repeatCount: Int = -1) {
        val animator = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
        animator.duration = duration
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.repeatCount = if (repeatCount == -1) ObjectAnimator.INFINITE else repeatCount
        animator.start()
    }

    /**
     * Anima o progresso de uma barra de progresso
     */
    fun animateProgress(view: View, fromProgress: Int, toProgress: Int, duration: Long = 1000) {
        if (view is ProgressBar) {
            val animator = ObjectAnimator.ofInt(fromProgress, toProgress)
            animator.duration = duration
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener { animation ->
                view.progress = animation.animatedValue as Int
            }
            animator.start()
        }
    }

    /**
     * Anima a mudança de cor de uma view
     */
    fun animateColorChange(view: View, fromColor: Int, toColor: Int, duration: Long = 500) {
        val animator = ObjectAnimator.ofArgb(fromColor, toColor)
        animator.duration = duration
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            view.setBackgroundColor(animation.animatedValue as Int)
        }
        animator.start()
    }

    /**
     * Anima o fade in de uma view
     */
    fun fadeIn(view: View, duration: Long = 300, delay: Long = 0) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /**
     * Anima o fade out de uma view
     */
    fun fadeOut(view: View, duration: Long = 300, onEnd: () -> Unit = {}) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    onEnd()
                }
            })
            .start()
    }

    /**
     * Anima uma view para aparecer com efeito de slide
     */
    fun slideInFromBottom(view: View, duration: Long = 300, delay: Long = 0) {
        view.translationY = view.height.toFloat()
        view.alpha = 0f
        view.visibility = View.VISIBLE
        
        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(duration)
            .setStartDelay(delay)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    /**
     * Anima uma view para desaparecer com efeito de slide
     */
    fun slideOutToBottom(view: View, duration: Long = 300, onEnd: () -> Unit = {}) {
        view.animate()
            .translationY(view.height.toFloat())
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    onEnd()
                }
            })
            .start()
    }

    /**
     * Cria uma animação de pulso para destacar elementos
     */
    fun pulse(view: View, duration: Long = 1000, repeatCount: Int = 3) {
        val animator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f)
        val animatorY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f)
        
        animator.duration = duration
        animator.repeatCount = repeatCount
        animator.interpolator = AccelerateDecelerateInterpolator()
        
        animatorY.duration = duration
        animatorY.repeatCount = repeatCount
        animatorY.interpolator = AccelerateDecelerateInterpolator()
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animator, animatorY)
        animatorSet.start()
    }

    /**
     * Anima uma view para aparecer com efeito de zoom
     */
    fun zoomIn(view: View, duration: Long = 300, delay: Long = 0) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0f
        view.visibility = View.VISIBLE
        
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(duration)
            .setStartDelay(delay)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    /**
     * Anima uma view para desaparecer com efeito de zoom
     */
    fun zoomOut(view: View, duration: Long = 300, onEnd: () -> Unit = {}) {
        view.animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    onEnd()
                }
            })
            .start()
    }

    /**
     * Anima FABs para sair da tela
     */
    fun animateFabMenuExit(fab: FloatingActionButton, onEnd: () -> Unit = {}) {
        fab.animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fab.visibility = View.GONE
                    onEnd()
                }
            })
            .start()
    }

    /**
     * Anima FABs para entrar na tela
     */
    fun animateFabMenuEnter(fab: FloatingActionButton) {
        fab.scaleX = 0f
        fab.scaleY = 0f
        fab.alpha = 0f
        fab.visibility = View.VISIBLE
        
        fab.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(200)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    /**
     * Anima elementos em cascata (um após o outro)
     */
    fun animateCascadeEnter(views: List<View>, delayBetweenViews: Long = 150) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay((index * delayBetweenViews).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
}