package io.tokend.template.view.util

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder

object ImageViewUtil {
    fun loadImage(
        target: ImageView,
        url: String?,
        placeholder: Drawable?,
        glideCustomization: RequestBuilder<*>.() -> Unit = {}
    ) {
        val glide = Glide.with(target.context)

        if (placeholder != null) {
            target.setImageDrawable(placeholder)
        }

        if (url != null) {
            glide
                .load(url)
                .placeholder(placeholder)
                .apply(glideCustomization)
                .into(target)
        } else {
            glide.clear(target)
            if (placeholder != null) {
                target.setImageDrawable(placeholder)
            }
        }
    }

    fun loadImageCircle(
        target: ImageView,
        url: String?,
        placeholder: Drawable?,
        glideCustomization: RequestBuilder<*>.() -> Unit = {}
    ) {
        loadImage(target, url, placeholder) {
            apply(glideCustomization)
            circleCrop()
        }
    }
}