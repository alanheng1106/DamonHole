package com.example.damonhole

import android.animation.ValueAnimator
import android.view.animation.Interpolator
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * PixelPlayer-style navigation transitions using Kotlin + Android Animator API.
 * Implements:
 *   - Parallax slide: exiting screen moves only 33%, entering moves 100%
 *   - Alpha blending: smooth fade overlay on both screens simultaneously
 *   - 380ms duration with FastOutSlowInInterpolator (Material easing curve)
 */
object NavigationTransitions {

    private const val DURATION_MS = 380L
    private val EASING: Interpolator = FastOutSlowInInterpolator()

    /**
     * Performs a forward tab switch animation (moving right: Home → Settings).
     * - Enter: slides in from +100% with fade in
     * - Exit: slides to -33% with fade out (parallax)
     */
    fun navigateForward(
        fm: FragmentManager,
        containerId: Int,
        target: Fragment
    ) {
        val tx = fm.beginTransaction()

        val currentFragment = fm.findFragmentById(containerId)

        tx.replace(containerId, target)
        tx.runOnCommit {
            val enterView = target.view ?: return@runOnCommit
            val screenWidth = enterView.resources.displayMetrics.widthPixels.toFloat()

            // Start position: fully off-screen to the right
            enterView.translationX = screenWidth
            enterView.alpha = 0f

            // Animate: slide in from right, fade in
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = DURATION_MS
                interpolator = EASING
                addUpdateListener { anim ->
                    val fraction = anim.animatedFraction
                    enterView.translationX = screenWidth * (1f - fraction)
                    enterView.alpha = fraction
                }
                start()
            }

            // Animate exit: parallax slide left (-33%) + fade out
            currentFragment?.view?.let { exitView ->
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = DURATION_MS
                    interpolator = EASING
                    addUpdateListener { anim ->
                        val fraction = anim.animatedFraction
                        exitView.translationX = -screenWidth * 0.33f * fraction
                        exitView.alpha = 1f - fraction
                    }
                    start()
                }
            }
        }
        tx.commit()
    }

    /**
     * Performs a backward tab switch animation (moving left: Settings → Home).
     * - Enter: slides in from -33% (parallax position) with scale 0.9→1.0 and fade in
     * - Exit: slides to +100% with scale 1.0→0.8 and fade out
     */
    fun navigateBackward(
        fm: FragmentManager,
        containerId: Int,
        target: Fragment
    ) {
        val tx = fm.beginTransaction()

        val currentFragment = fm.findFragmentById(containerId)

        tx.replace(containerId, target)
        tx.runOnCommit {
            val enterView = target.view ?: return@runOnCommit
            val screenWidth = enterView.resources.displayMetrics.widthPixels.toFloat()

            // Start position: parallax position (-33%) with slight scale down
            enterView.translationX = -screenWidth * 0.33f
            enterView.alpha = 0f
            enterView.scaleX = 0.9f
            enterView.scaleY = 0.9f

            // Animate: slide in from parallax, scale up, fade in
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = DURATION_MS
                interpolator = EASING
                addUpdateListener { anim ->
                    val fraction = anim.animatedFraction
                    enterView.translationX = -screenWidth * 0.33f * (1f - fraction)
                    enterView.alpha = fraction
                    enterView.scaleX = 0.9f + 0.1f * fraction
                    enterView.scaleY = 0.9f + 0.1f * fraction
                }
                start()
            }

            // Animate exit: slide to +100%, scale down to 0.8, fade out
            currentFragment?.view?.let { exitView ->
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = DURATION_MS
                    interpolator = EASING
                    addUpdateListener { anim ->
                        val fraction = anim.animatedFraction
                        exitView.translationX = screenWidth * fraction
                        exitView.alpha = 1f - fraction
                        exitView.scaleX = 1f - 0.2f * fraction
                        exitView.scaleY = 1f - 0.2f * fraction
                    }
                    start()
                }
            }
        }
        tx.commit()
    }
}
