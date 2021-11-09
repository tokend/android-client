package io.tokend.template.view.util

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.RotateAnimation

/**
 * Contains utilities for view animation
 */
object AnimationUtil {
    /**
     * Animates view height from 0 to the actual one
     */
    fun expandView(view: View?) {
        view ?: return

        view.clearAnimation()

        view.visibility = View.VISIBLE
        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(widthSpec, heightSpec)

        val animator = getHeightAnimator(0, view.measuredHeight, view)
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator?) {
                val layoutParams = view.layoutParams
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                view.layoutParams = layoutParams
            }

            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {}
        })
        animator.start()
    }

    /**
     * Animates view height from the actual one to 0
     */
    fun collapseView(view: View?, onEnd: (() -> Unit)? = null) {
        view ?: return

        view.clearAnimation()

        val height = view.height
        val animator = getHeightAnimator(height, 0, view)
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator?) {
                view.visibility = View.GONE
                onEnd?.invoke()
            }

            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {}
        })
        animator.start()
    }

    /**
     * Animates view opacity from 0 to 1
     */
    fun fadeInView(view: View?, duration: Long = -1) {
        view ?: return

        view.clearAnimation()
        view.visibility = View.VISIBLE

        val alphaAnimation = AlphaAnimation(0f, 1f)
        alphaAnimation.interpolator = AccelerateDecelerateInterpolator()
        alphaAnimation.duration =
            if (duration == -1L)
                view.context.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
            else
                duration

        alphaAnimation.fillAfter = true

        view.startAnimation(alphaAnimation)
    }

    /**
     * Animates view opacity from 1 to 0
     */
    fun fadeOutView(
        view: View?,
        duration: Long = -1,
        viewGone: Boolean = true,
        onEnd: (() -> Unit)? = null
    ) {
        view ?: return
        if (view.visibility == View.GONE
            || view.visibility == View.INVISIBLE
            || view.alpha == 0f
        ) {
            return
        }
        view.clearAnimation()

        val alphaAnimation = AlphaAnimation(1f, 0f)
        alphaAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation?) {
                if (viewGone) view.visibility = View.GONE
                onEnd?.invoke()
            }

            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationStart(animation: Animation?) {}

        })
        alphaAnimation.interpolator = AccelerateDecelerateInterpolator()
        alphaAnimation.duration =
            if (duration == -1L)
                view.context.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
            else
                duration
        alphaAnimation.fillAfter = true
        view.startAnimation(alphaAnimation)
    }

    private fun getHeightAnimator(from: Int, to: Int, view: View): ValueAnimator {
        val animator = ValueAnimator.ofInt(from, to)
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration =
            view.context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        animator.addUpdateListener {
            val value = animator.animatedValue as Int
            val layoutParams = view.layoutParams
            layoutParams.height = value
            view.layoutParams = layoutParams
        }
        return animator
    }

    /**
     * Animates view rotation around it's center
     */
    fun rotateView(view: View?, from: Float, to: Float) {
        view ?: return

        val rotateAnimation = RotateAnimation(
            from, to,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        )
        rotateAnimation.interpolator = AccelerateDecelerateInterpolator()
        rotateAnimation.duration =
            view.context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        rotateAnimation.fillAfter = true

        view.clearAnimation()
        view.startAnimation(rotateAnimation)
    }
}