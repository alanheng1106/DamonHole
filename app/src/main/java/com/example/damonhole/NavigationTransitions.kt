package com.example.damonhole

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.Interpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * PixelPlayer-exact navigation transitions.
 * - Forward: enter slides 100% from right + fade; exit slides -33% (parallax) + fade
 * - Backward: enter from -33% with scale 0.9→1.0 + fade; exit to +100% with scale 1.0→0.75 + fade
 * Both animations run simultaneously on the same ValueAnimator for a true layered parallax feel.
 */
object NavigationTransitions {

    private const val DURATION_MS = 350L
    private val EASING: Interpolator = FastOutSlowInInterpolator()

    fun switchTab(
        fm: FragmentManager,
        containerId: Int,
        currentFragment: Fragment?,
        targetFragment: Fragment,
        isForward: Boolean
    ) {
        // Capture current view reference BEFORE transaction
        val currentView = currentFragment?.view

        val tx = fm.beginTransaction()
        if (!targetFragment.isAdded) {
            tx.add(containerId, targetFragment)
        } else {
            tx.show(targetFragment)
        }

        tx.runOnCommit {
            val targetView = targetFragment.view ?: return@runOnCommit
            val w = targetView.resources.displayMetrics.widthPixels.toFloat()

            // Set initial state for entering view
            if (isForward) {
                targetView.translationX = w
                targetView.alpha = 0f
            } else {
                targetView.translationX = -w * 0.33f
                targetView.scaleX = 0.9f
                targetView.scaleY = 0.9f
                targetView.alpha = 0f
            }

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
                if (isForward) {
                    currentView?.translationX = -w * 0.33f * f
                    currentView?.alpha = 1f - f
                } else {
                    currentView?.translationX = w * f
                    currentView?.alpha = 1f - f
                    currentView?.scaleX = 1f - 0.25f * f
                    currentView?.scaleY = 1f - 0.25f * f
                }
            }

            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Reset exiting view, then hide it
                    currentView?.apply {
                        translationX = 0f; alpha = 1f
                        scaleX = 1f; scaleY = 1f
                    }
                    if (currentFragment != null && currentFragment.isAdded) {
                        fm.beginTransaction().hide(currentFragment).commitAllowingStateLoss()
                    }
                }
            })

            anim.start()
        }
        tx.commitAllowingStateLoss()
    }
}
