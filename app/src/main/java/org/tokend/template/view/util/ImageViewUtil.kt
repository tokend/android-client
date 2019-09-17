package org.tokend.template.view.util

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import org.tokend.template.util.imagetransform.CircleTransform

object ImageViewUtil {
    fun loadImage(target: ImageView,
                  url: String?,
                  placeholder: Drawable?,
                  picassoCustomization: RequestCreator.() -> Unit = {}) {
        val picasso = Picasso.with(target.context)

        if (placeholder != null) {
            target.setImageDrawable(placeholder)
        }

        if (url != null) {
            picasso
                    .load(url)
                    .placeholder(placeholder)
                    .apply(picassoCustomization)
                    .into(target)
        } else {
            picasso.cancelRequest(target)
            if (placeholder != null) {
                target.setImageDrawable(placeholder)
            }
        }
    }

    fun loadImageCircle(target: ImageView,
                        url: String?,
                        placeholder: Drawable?,
                        picassoCustomization: RequestCreator.() -> Unit = {}) {
        loadImage(target, url, placeholder) {
            apply(picassoCustomization)
            transform(CircleTransform())
            fit()
            centerCrop()
        }
    }
}