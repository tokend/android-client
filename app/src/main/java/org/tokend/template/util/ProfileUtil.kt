package org.tokend.template.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import com.squareup.picasso.Picasso
import org.tokend.template.R
import org.tokend.template.di.providers.UrlConfigProvider
import org.tokend.template.features.assets.LogoFactory
import org.tokend.template.features.kyc.model.KycState
import org.tokend.template.features.kyc.model.form.SimpleKycForm
import org.tokend.template.features.kyc.storage.SubmittedKycStatePersistor
import org.tokend.template.util.imagetransform.CircleTransform

object ProfileUtil {

    fun getAvatarUrl(kycState: KycState?,
                     urlConfigProvider: UrlConfigProvider): String? {
        val submittedForm = (kycState as? KycState.Submitted<*>)?.formData
        val avatar = (submittedForm as? SimpleKycForm)?.avatar
        return avatar?.getUrl(urlConfigProvider.getConfig().storage)
    }

    fun initAccountLogo(context: Context,
                        view: AppCompatImageView,
                        email: String,
                        urlConfigProvider: UrlConfigProvider,
                        kycStatePersistor: SubmittedKycStatePersistor) {

        val placeholderImage = LogoFactory(context)
                .getForValue(
                        email.toUpperCase(),
                        context.resources.getDimensionPixelSize(R.dimen.hepta_margin),
                        ContextCompat.getColor(context, R.color.avatar_placeholder_background),
                        Color.WHITE
                )

        val placeholderDrawable = BitmapDrawable(context.resources, placeholderImage)
        view.setImageDrawable(placeholderDrawable)

        getAvatarUrl(kycStatePersistor.loadState(), urlConfigProvider)?.let {
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