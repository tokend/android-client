package io.tokend.template.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.Dimension
import androidx.core.content.ContextCompat
import io.tokend.template.R
import io.tokend.template.di.providers.UrlConfigProvider
import io.tokend.template.features.kyc.model.ActiveKyc
import io.tokend.template.features.kyc.model.KycForm
import io.tokend.template.features.kyc.model.KycFormWithAvatar
import io.tokend.template.view.util.ImageViewUtil
import io.tokend.template.view.util.LogoFactory
import java.util.*

object ProfileUtil {

    fun getAvatarUrl(
        activeKyc: ActiveKyc?,
        urlConfigProvider: UrlConfigProvider
    ): String? {
        return getAvatarUrl((activeKyc as? ActiveKyc.Form)?.formData, urlConfigProvider)
    }

    fun getAvatarUrl(
        kycForm: KycForm?,
        urlConfigProvider: UrlConfigProvider
    ): String? {
        return (kycForm as? KycFormWithAvatar)?.avatar?.getUrl(urlConfigProvider.getConfig().storage)
    }

    fun getAvatarPlaceholder(
        login: String,
        context: Context,
        @Dimension
        sizePx: Int
    ): Drawable {
        val placeholderImage = LogoFactory(context)
            .getForValue(
                login.toUpperCase(Locale.ENGLISH),
                sizePx,
                ContextCompat.getColor(context, R.color.avatar_placeholder_background),
                Color.WHITE
            )

        return BitmapDrawable(context.resources, placeholderImage)
    }

    fun setAvatar(
        view: ImageView,
        email: String,
        urlConfigProvider: UrlConfigProvider,
        activeKyc: ActiveKyc?,
        sizePx: Int = (view.layoutParams as ViewGroup.LayoutParams).width
    ) {
        val placeholderDrawable = getAvatarPlaceholder(email, view.context, sizePx)
        val avatarUrl = getAvatarUrl(activeKyc, urlConfigProvider)

        ImageViewUtil.loadImageCircle(view, avatarUrl, placeholderDrawable)
    }
}