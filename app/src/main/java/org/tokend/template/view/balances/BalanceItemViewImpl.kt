package org.tokend.template.view.balances

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.list_item_balance.view.*
import org.tokend.template.R
import org.tokend.template.features.assets.LogoFactory
import org.tokend.template.util.imagetransform.CircleTransform

class BalanceItemViewImpl(view: View) : BalanceItemView {
    private val logoSize =
            view.context.resources.getDimensionPixelSize(R.dimen.asset_list_item_logo_size)
    private val logoFactory = LogoFactory(view.context)
    private val picasso = Picasso.with(view.context)

    override val logoImageView: ImageView = view.asset_logo_image_view
    override val nameTextView: TextView = view.asset_code_text_view
    override val amountTextView: TextView = view.balance_available_text_view
    override val dividerView: View = view.divider_view

    override fun displayLogo(logoUrl: String?, assetCode: String) {
        if (logoUrl != null) {
            picasso.load(logoUrl)
                    .placeholder(R.color.white)
                    .resize(logoSize, logoSize)
                    .centerInside()
                    .transform(CircleTransform())
                    .into(logoImageView)
        } else {
            picasso.cancelRequest(logoImageView)
            logoImageView.setImageBitmap(
                    logoFactory.getWithAutoBackground(
                            assetCode,
                            logoSize
                    )
            )
        }
    }
}