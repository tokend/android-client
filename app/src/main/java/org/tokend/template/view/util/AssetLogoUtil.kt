package org.tokend.template.view.util

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.annotation.Dimension
import android.widget.ImageView
import com.squareup.picasso.Picasso
import org.tokend.template.data.model.AssetRecord
import org.tokend.template.features.assets.LogoFactory
import org.tokend.template.util.imagetransform.CircleTransform

object AssetLogoUtil {
    private fun generateAssetLogo(assetCode: String,
                                  context: Context,
                                  @Dimension
                                  logoSize: Int): Drawable {
        val bitmap = LogoFactory(context).getWithAutoBackground(
                assetCode,
                logoSize
        )

        return BitmapDrawable(context.resources, bitmap)
    }

    fun setAssetLogo(view: ImageView,
                     asset: AssetRecord,
                     @Dimension
                     logoSize: Int) {
        setAssetLogo(view, asset.code, asset.logoUrl, logoSize)
    }

    fun setAssetLogo(view: ImageView,
                     assetCode: String,
                     logoUrl: String?,
                     @Dimension
                     logoSize: Int) {
        val context = view.context
        val picasso = Picasso.with(context)

        if (logoUrl != null) {
            picasso.load(logoUrl)
                    .resize(logoSize, logoSize)
                    .centerInside()
                    .transform(CircleTransform())
                    .into(view)
        } else {
            picasso.cancelRequest(view)
            view.setImageDrawable(generateAssetLogo(assetCode, context, logoSize))
        }
    }
}