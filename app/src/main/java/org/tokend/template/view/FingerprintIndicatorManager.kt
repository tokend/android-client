package org.tokend.template.view

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import android.view.View
import android.view.animation.AnimationUtils
import org.tokend.template.R
import org.tokend.template.view.util.AnimationUtil

class FingerprintIndicatorManager(context: Context,
                                  private val fingerprintIndicator: AppCompatImageView,
                                  private val toastManager: ToastManager? = null) {

    private val animation = AnimationUtils.loadAnimation(context, R.anim.shake)

    init {
        fingerprintIndicator.setOnClickListener {
            toastManager?.short(R.string.touch_sensor)
        }
    }

    fun hide() {
        fingerprintIndicator.visibility = View.GONE
    }

    fun show() {
        AnimationUtil.fadeInView(fingerprintIndicator)
    }

    fun error() {
        fingerprintIndicator.clearAnimation()
        fingerprintIndicator.startAnimation(animation)
    }
}