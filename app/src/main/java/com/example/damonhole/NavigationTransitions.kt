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
 * Uses add/show/hide with OnPreDrawListener to guarantee no flash.
 */
object NavigationTransitions {

    private const val DURATION_MS = 350L
    private val EASING: Interpolator = FastOutSlowInInterpolator()
    private var activeAnimator: ValueAnimator? = null

    fun switchTab(
        fm: FragmentManager,
        containerId: Int,
        currentFragment: Fragment?,
        targetFragment: Fragment,
        isForward: Boolean
    ) {
        // Cancel any in-progress animation and snap everything clean
        activeAnimator?.let { anim ->
            anim.removeAllUpdateListeners()
            anim.removeAllListeners()
            anim.cancel()
            activeAnimator = null
        }

        // Reset all fragment views to clean state
        fm.fragments.forEach { frag ->
            frag.view?.apply {
                translationX = 0f
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
                visibility = if (frag === currentFragment || frag === targetFragment) View.VISIBLE else View.GONE
            }
        }

        // Capture exit view BEFORE any transaction
        val exitView = currentFragment?.view

        // Add or show the target fragment
        val tx = fm.beginTransaction()
        if (!targetFragment.isAdded) {
            tx.add(containerId, targetFragment)
        } else {
            tx.show(targetFragment)
        }
        // Don't hide current yet — we need it for exit animation
        tx.commitNow()

        val enterView = targetFragment.view ?: return
        val w = enterView.resources.displayMetrics.widthPixels.toFloat()

        // Immediately set enter view off-screen and invisible (before any draw)
        if (isForward) {
            enterView.translationX = w
        } else {
            enterView.translationX = -w * 0.33f
            enterView.scaleX = 0.9f
            enterView.scaleY = 0.9f
        }
        enterView.alpha = 0f

        // Start animation on next frame via post (view is already alpha=0 so no flash)
        enterView.post {
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = DURATION_MS
                interpolator = EASING
            }

            anim.addUpdateListener { va ->
                val f = va.animatedFraction
                // Enter
                if (isForward) {
                    enterView.translationX = w * (1f - f)
                    enterView.alpha = f
                } else {
                    enterView.translationX = -w * 0.33f * (1f - f)
                    enterView.alpha = f
                    enterView.scaleX = 0.9f + 0.1f * f
                    enterView.scaleY = 0.9f + 0.1f * f
                }
                // Exit (parallax)
                exitView?.let { ev ->
                    if (isForward) {
                        ev.translationX = -w * 0.33f * f
                        ev.alpha = 1f - f
                    } else {
                        ev.translationX = w * f
                        ev.alpha = 1f - f
                        ev.scaleX = 1f - 0.25f * f
                        ev.scaleY = 1f - 0.25f * f
                    }
                }
            }

            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    activeAnimator = null
                    // Reset and hide exiting fragment
                    exitView?.apply {
                        translationX = 0f; alpha = 1f
                        scaleX = 1f; scaleY = 1f
                    }
                    if (currentFragment != null && currentFragment.isAdded) {
                        fm.beginTransaction().hide(currentFragment).commitAllowingStateLoss()
                    }
                    // Ensure enter view is at rest
                    enterView.translationX = 0f
                    enterView.alpha = 1f
                    enterView.scaleX = 1f
                    enterView.scaleY = 1f
                }
            })

            activeAnimator = anim
            anim.start()
        }
    }
}
