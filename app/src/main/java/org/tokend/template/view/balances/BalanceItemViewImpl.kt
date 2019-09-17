package org.tokend.template.view.balances

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.list_item_balance.view.*
import org.tokend.template.view.util.CircleLogoUtil

class BalanceItemViewImpl(view: View) : BalanceItemView {
    override val logoImageView: ImageView = view.asset_logo_image_view
    override val nameTextView: TextView = view.asset_code_text_view
    override val amountTextView: TextView = view.balance_available_text_view
    override val dividerView: View = view.divider_view

    override fun displayLogo(logoUrl: String?, assetCode: String) {
        CircleLogoUtil.setLogo(logoImageView, assetCode, logoUrl)
    }
}