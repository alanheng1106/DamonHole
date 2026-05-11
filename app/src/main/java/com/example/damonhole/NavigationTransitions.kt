package com.example.damonhole

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.Interpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * PixelPlayer-style navigation transitions.
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
        // 1. Cancel any in-progress animation and snap clean
        activeAnimator?.let { anim ->
            anim.removeAllUpdateListeners()
            anim.removeAllListeners()
            anim.cancel()
            activeAnimator = null
        }

        // 2. Clean up all fragment views
        fm.fragments.forEach { frag ->
            frag.view?.apply {
                translationX = 0f; alpha = 1f; scaleX = 1f; scaleY = 1f
            }
        }

        // 3. BEFORE the transaction: pre-hide target's existing view
        //    so show() can't flash it (show() only sets visibility, not alpha)
        targetFragment.view?.alpha = 0f

        // 4. Capture exit view reference
        val exitView = currentFragment?.view

        // 5. Perform fragment transaction
        val tx = fm.beginTransaction()
        if (!targetFragment.isAdded) {
            tx.add(containerId, targetFragment)
        } else {
            tx.show(targetFragment)
        }
        tx.commitNow()

        // 6. Get enter view (now guaranteed to exist after commitNow)
        val enterView = targetFragment.view ?: return
        val w = enterView.resources.displayMetrics.widthPixels.toFloat()

        // 7. Set initial off-screen state (alpha already 0 from step 3 or default)
        enterView.alpha = 0f
        if (isForward) {
            enterView.translationX = w
            enterView.scaleX = 1f
            enterView.scaleY = 1f
        } else {
            enterView.translationX = -w * 0.33f
            enterView.scaleX = 0.9f
            enterView.scaleY = 0.9f
        }

        // 8. Animate on next frame
        enterView.post {
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = DURATION_MS
                interpolator = EASING
            }

            anim.addUpdateListener { va ->
                val f = va.animatedFraction
                if (isForward) {
                    enterView.translationX = w * (1f - f)
                    enterView.alpha = f
                } else {
                    enterView.translationX = -w * 0.33f * (1f - f)
                    enterView.alpha = f
                    enterView.scaleX = 0.9f + 0.1f * f
                    enterView.scaleY = 0.9f + 0.1f * f
                }
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
                    exitView?.apply {
                        translationX = 0f; alpha = 1f; scaleX = 1f; scaleY = 1f
                    }
                    if (currentFragment != null && currentFragment.isAdded) {
                        fm.beginTransaction().hide(currentFragment).commitAllowingStateLoss()
                    }
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
