package org.tokend.template.features.localaccount.view.util

import android.view.ViewGroup
import android.widget.ImageView
import org.tokend.template.features.localaccount.model.LocalAccount

object LocalAccountLogoUtil {
    private val generator = PixelSquareLogoGenerator()

    fun setLogo(view: ImageView,
                account: LocalAccount,
                sizePx: Int = (view.layoutParams as ViewGroup.LayoutParams).width) {
        view.setImageBitmap(
                generator.generate(account.accountId.toByteArray(), sizePx)
        )
    }
}