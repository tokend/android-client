package org.tokend.template.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.annotation.Dimension
import android.support.v4.content.ContextCompat
import android.widget.ImageView
import com.squareup.picasso.Picasso
import org.tokend.template.R
import org.tokend.template.di.providers.UrlConfigProvider
import org.tokend.template.features.assets.LogoFactory
import org.tokend.template.features.kyc.model.KycState
import org.tokend.template.features.kyc.model.form.SimpleKycForm
import org.tokend.template.util.imagetransform.CircleTransform

object ProfileUtil {

    fun getAvatarUrl(kycState: KycState?,
                     urlConfigProvider: UrlConfigProvider): String? {
        val submittedForm = (kycState as? KycState.Submitted<*>)?.formData
        val avatar = (submittedForm as? SimpleKycForm)?.avatar
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
                  savedKycState: KycState.Submitted<*>?) {
        val context = view.context

        val placeholderDrawable = getAvatarPlaceholder(
                email, context, context.resources.getDimensionPixelSize(R.dimen.hepta_margin)
        )
        view.setImageDrawable(placeholderDrawable)

        getAvatarUrl(savedKycState, urlConfigProvider)?.let {
            Picasso.with(context)
                    .load(it)
                    .placeholder(placeholderDrawable)
                    .transform(CircleTransform())
                    .fit()
                    .centerCrop()
                    .into(view)
        }
    }
}