package org.tokend.template.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.annotation.Dimension
import android.support.v4.content.ContextCompat
import android.view.ViewGroup
import android.widget.ImageView
import org.tokend.template.R
import org.tokend.template.di.providers.UrlConfigProvider
import org.tokend.template.features.assets.LogoFactory
import org.tokend.template.features.kyc.model.KycForm
import org.tokend.template.features.kyc.model.KycState
import org.tokend.template.view.util.ImageViewUtil

object ProfileUtil {

    fun getAvatarUrl(kycState: KycState?,
                     urlConfigProvider: UrlConfigProvider): String? {
        val submittedForm = (kycState as? KycState.Submitted<*>)?.formData
        val avatar = when (submittedForm) {
            is KycForm.Corporate -> submittedForm.avatar
            is KycForm.General -> submittedForm.avatar
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
                  savedKycState: KycState.Submitted<*>?,
                  sizePx: Int = (view.layoutParams as ViewGroup.LayoutParams).width) {
        val placeholderDrawable = getAvatarPlaceholder(email, view.context, sizePx)
        val avatarUrl = getAvatarUrl(savedKycState, urlConfigProvider)

        ImageViewUtil.loadImageCircle(view, avatarUrl, placeholderDrawable)
    }
}