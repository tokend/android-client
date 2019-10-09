package org.tokend.template.features.localaccount.view.util

import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.view.ViewGroup
import android.widget.ImageView
import org.tokend.template.R
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.view.util.ImageViewUtil

object LocalAccountLogoUtil {
    fun setLogo(view: ImageView,
                account: LocalAccount,
                sizePx: Int = (view.layoutParams as ViewGroup.LayoutParams).width) {
        val placeholder = ColorDrawable(ContextCompat.getColor(view.context, R.color.avatar_placeholder_background))

        val logoCode = account.accountId
        val logoUrl = "https://www.tinygraphs.com/squares/$logoCode" +
                "?size=$sizePx" +
                "&fmt=jpeg&theme=bythepool"

        ImageViewUtil.loadImage(view, logoUrl, placeholder)
    }
}