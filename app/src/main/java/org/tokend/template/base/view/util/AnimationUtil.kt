package org.tokend.template.base.view.util

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.RotateAnimation

object AnimationUtil {
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

    fun fadeInView(view: View?) {
        view ?: return

        view.visibility = View.VISIBLE

        val alphaAnimation = AlphaAnimation(0f, 1f)
        alphaAnimation.interpolator = AccelerateDecelerateInterpolator()
        alphaAnimation.duration =
                view.context.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        alphaAnimation.fillAfter = true

        view.clearAnimation()
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

    fun rotateView(view: View?, from: Float, to: Float) {
        view ?: return

        val rotateAnimation = RotateAnimation(from, to,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        rotateAnimation.interpolator = AccelerateDecelerateInterpolator()
        rotateAnimation.duration =
                view.context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        rotateAnimation.fillAfter = true

        view.clearAnimation()
        view.startAnimation(rotateAnimation)
    }
}