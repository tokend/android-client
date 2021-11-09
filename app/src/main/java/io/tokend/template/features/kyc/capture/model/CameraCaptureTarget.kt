package io.tokend.template.features.kyc.capture.model

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import io.tokend.template.R
import io.tokend.template.extensions.forEachChild

enum class CameraCaptureTarget {
    SELFIE,
    ;

    fun getCaptureOverlay(
        context: Context,
        parent: ViewGroup
    ): ViewGroup {
        val layoutInflater = LayoutInflater.from(context)

        return when (this) {
            SELFIE ->
                layoutInflater.inflate(R.layout.capture_overlay_selfie, parent, false)
        } as ViewGroup
    }

    @SuppressLint("RestrictedApi")
    fun getPreviewOverlay(
        context: Context,
        parent: ViewGroup,
        backgroundColor: Int
    ): ViewGroup {
        val layoutInflater = LayoutInflater.from(context)

        val overlayLayout = when (this) {
            SELFIE ->
                layoutInflater.inflate(R.layout.capture_overlay_selfie, parent, false)
        } as ViewGroup

        overlayLayout.forEachChild {
            if (it::class.java == View::class.java) {
                it.visibility = View.INVISIBLE
            }
        }

        overlayLayout.findViewById<AppCompatImageView>(R.id.mask_image_view).apply {
            supportImageTintList = ColorStateList.valueOf(backgroundColor)
            alpha = 1f
        }

        return overlayLayout
    }

    fun getCaptureHint(context: Context): String? {
        return null
    }

    fun getRetryLabel(context: Context): String {
        return when (this) {
            SELFIE -> context.getString(R.string.take_new_selfie)
        }
    }

    fun getAcceptLabel(context: Context): String {
        return when (this) {
            SELFIE -> context.getString(R.string.my_selfie_is_clear)
        }
    }
}