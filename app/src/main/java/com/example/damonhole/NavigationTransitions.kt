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
 * Uses OnPreDrawListener to guarantee no flash on first frame.
 */
object NavigationTransitions {

    private const val DURATION_MS = 350L
    private val EASING: Interpolator = FastOutSlowInInterpolator()
    private var activeAnimator: ValueAnimator? = null
    private var isAnimating = false

    fun switchTab(
        fm: FragmentManager,
        containerId: Int,
        currentFragment: Fragment?,
        targetFragment: Fragment,
        isForward: Boolean
    ) {
        // If already animating, snap to end immediately
        activeAnimator?.let { anim ->
            anim.removeAllUpdateListeners()
            anim.removeAllListeners()
            anim.cancel()
            activeAnimator = null
        }

        // Reset ALL managed fragment views to clean state
        fm.fragments.forEach { frag ->
            frag.view?.apply {
                translationX = 0f
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
            }
        }

        // Hide all other fragments except target
        val tx = fm.beginTransaction()
        fm.fragments.forEach { frag ->
            if (frag !== targetFragment && frag.isAdded) {
                tx.hide(frag)
            }
        }

        // Capture current view BEFORE hiding
        val currentView = currentFragment?.view

        if (!targetFragment.isAdded) {
            tx.add(containerId, targetFragment)
        } else {
            tx.show(targetFragment)
        }
        tx.commitNow()

        val targetView = targetFragment.view ?: return
        val w = targetView.resources.displayMetrics.widthPixels.toFloat()

        // Make target invisible to prevent any flash
        targetView.visibility = View.INVISIBLE

        // If we have a currentView for exit animation, show it again temporarily
        // (we hid it in the transaction above, but we need it visible for animation)
        if (currentView != null && currentFragment != null && currentFragment.isAdded) {
            fm.beginTransaction().show(currentFragment).commitNow()
        }

        // Use OnPreDrawListener: intercept the FIRST draw to set initial state
        targetView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                targetView.viewTreeObserver.removeOnPreDrawListener(this)

                // Set initial transforms
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
                targetView.visibility = View.VISIBLE

                // Start animation
                val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = DURATION_MS
                    interpolator = EASING
                }

                anim.addUpdateListener { va ->
                    val f = va.animatedFraction
                    // Enter
                    if (isForward) {
                        targetView.translationX = w * (1f - f)
                        targetView.alpha = f
                    } else {
                        targetView.translationX = -w * 0.33f * (1f - f)
                        targetView.alpha = f
                        targetView.scaleX = 0.9f + 0.1f * f
                        targetView.scaleY = 0.9f + 0.1f * f
                    }
                    // Exit (parallax)
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
                        isAnimating = false
                        // Reset and hide exiting fragment
                        currentView?.apply {
                            translationX = 0f; alpha = 1f
                            scaleX = 1f; scaleY = 1f
                        }
                        if (currentFragment != null && currentFragment.isAdded) {
                            fm.beginTransaction().hide(currentFragment).commitAllowingStateLoss()
                        }
                        // Snap target to final state
                        targetView.translationX = 0f
                        targetView.alpha = 1f
                        targetView.scaleX = 1f
                        targetView.scaleY = 1f
                    }
                })

                activeAnimator = anim
                isAnimating = true
                anim.start()

                return false // skip this draw frame, draw on next with correct state
            }
        })

        // Request a layout to trigger the OnPreDrawListener
        targetView.invalidate()
    }
}
