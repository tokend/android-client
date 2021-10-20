package org.tokend.template.view.util

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView

object CircleLogoUtil {
    private fun generateLogo(content: String,
                             context: Context,
                             sizePx: Int): Drawable {
        return BitmapDrawable(
                context.resources,
                LogoFactory(context).getWithAutoBackground(content, sizePx)
        )
    }

    fun setLogo(view: ImageView,
                content: String,
                logoUrl: String?,
                sizePx: Int = (view.layoutParams as ViewGroup.LayoutParams).width) {
        val placeholder = generateLogo(content, view.context, sizePx)

        ImageViewUtil.loadImage(view, logoUrl, placeholder) {
            override(sizePx, sizePx)
            circleCrop()
        }
    }
}