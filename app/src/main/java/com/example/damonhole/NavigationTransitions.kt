package com.example.damonhole

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.Interpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * PixelPlayer-style navigation transitions.
 * - Forward: enter slides 100% from right + fade; exit slides -33% (parallax) + fade
 * - Backward: enter from -33% with scale 0.9→1.0 + fade; exit to +100% with scale 1.0→0.75 + fade
 */
object NavigationTransitions {

    private const val DURATION_MS = 350L
    private val EASING: Interpolator = FastOutSlowInInterpolator()

    // Track the active animator so we can cancel on rapid switching
    private var activeAnimator: ValueAnimator? = null

    fun switchTab(
        fm: FragmentManager,
        containerId: Int,
        currentFragment: Fragment?,
        targetFragment: Fragment,
        isForward: Boolean
    ) {
        // Cancel any in-progress animation and snap to final state
        activeAnimator?.let { anim ->
            anim.removeAllListeners()
            anim.removeAllUpdateListeners()
            anim.cancel()
            activeAnimator = null
        }

        // Reset ALL fragment views to clean state before starting
        val currentView = currentFragment?.view
        currentView?.apply {
            translationX = 0f; alpha = 1f
            scaleX = 1f; scaleY = 1f
        }

        // Hide current fragment immediately (no flash)
        if (currentFragment != null && currentFragment.isAdded && currentFragment !== targetFragment) {
            // Don't hide yet — we need its view for the exit animation.
            // Instead, we'll hide it in onAnimationEnd.
        }

        val tx = fm.beginTransaction()
        if (!targetFragment.isAdded) {
            tx.add(containerId, targetFragment)
        } else {
            tx.show(targetFragment)
        }
        tx.commitNow() // commitNow so the view is available immediately

        val targetView = targetFragment.view ?: return

        // ── Set initial state IMMEDIATELY (before the next frame draw) ──
        // This prevents the "flash" because the view never renders at its default position.
        val w = targetView.resources.displayMetrics.widthPixels.toFloat()
        if (isForward) {
            targetView.translationX = w
            targetView.alpha = 0f
            targetView.scaleX = 1f
            targetView.scaleY = 1f
        } else {
            targetView.translationX = -w * 0.33f
            targetView.scaleX = 0.9f
            targetView.scaleY = 0.9f
            targetView.alpha = 0f
        }

        // ── Start animation on the next frame ──
        targetView.post {
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = DURATION_MS
                interpolator = EASING
            }

            anim.addUpdateListener { va ->
                val f = va.animatedFraction
                // ── Enter ──
                if (isForward) {
                    targetView.translationX = w * (1f - f)
                    targetView.alpha = f
                } else {
                    targetView.translationX = -w * 0.33f * (1f - f)
                    targetView.alpha = f
                    targetView.scaleX = 0.9f + 0.1f * f
                    targetView.scaleY = 0.9f + 0.1f * f
                }
                // ── Exit (parallax) ──
                currentView?.let { cv ->
                    if (isForward) {
                        cv.translationX = -w * 0.33f * f
                        cv.alpha = 1f - f
                    } else {
                        cv.translationX = w * f
                        cv.alpha = 1f - f
                        cv.scaleX = 1f - 0.25f * f
                        cv.scaleY = 1f - 0.25f * f
                    }
                }
            }

            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    activeAnimator = null
                    // Reset exiting view and hide it
                    currentView?.apply {
                        translationX = 0f; alpha = 1f
                        scaleX = 1f; scaleY = 1f
                    }
                    if (currentFragment != null && currentFragment.isAdded) {
                        fm.beginTransaction().hide(currentFragment).commitAllowingStateLoss()
                    }
                    // Ensure target is at final resting state
                    targetView.translationX = 0f
                    targetView.alpha = 1f
                    targetView.scaleX = 1f
                    targetView.scaleY = 1f
                }
            })

            activeAnimator = anim
            anim.start()
        }
    }
}
