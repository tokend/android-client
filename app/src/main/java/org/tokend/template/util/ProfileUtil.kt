package org.tokend.template.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.Dimension
import androidx.core.content.ContextCompat
import android.view.ViewGroup
import android.widget.ImageView
import org.tokend.template.R
import org.tokend.template.di.providers.UrlConfigProvider
import org.tokend.template.features.kyc.model.ActiveKyc
import org.tokend.template.features.kyc.model.KycForm
import org.tokend.template.view.util.ImageViewUtil
import org.tokend.template.view.util.LogoFactory

object ProfileUtil {

    fun getAvatarUrl(activeKyc: ActiveKyc?,
                     urlConfigProvider: UrlConfigProvider): String? {
        val avatar = when (val form = (activeKyc as? ActiveKyc.Form)?.formData) {
            is KycForm.Corporate -> form.avatar
            is KycForm.General -> form.avatar
            else -> null
        }
        return avatar?.getUrl(urlConfigProvider.getConfig().storage)
    }

    fun getAvatarPlaceholder(email: String,
                             context: Context,
                             @Dimension
                             sizePx: Int): Drawable {
        val placeholderImage = LogoFactory(context)
                .getForValue(
                        email.toUpperCase(),
                        sizePx,
                        ContextCompat.getColor(context, R.color.avatar_placeholder_background),
                        Color.WHITE
                )

        return BitmapDrawable(context.resources, placeholderImage)
    }

    fun setAvatar(view: ImageView,
                  email: String,
                  urlConfigProvider: UrlConfigProvider,
                  activeKyc: ActiveKyc?,
                  sizePx: Int = (view.layoutParams as ViewGroup.LayoutParams).width) {
        val placeholderDrawable = getAvatarPlaceholder(email, view.context, sizePx)
        val avatarUrl = getAvatarUrl(activeKyc, urlConfigProvider)

        ImageViewUtil.loadImageCircle(view, avatarUrl, placeholderDrawable)
    }
}