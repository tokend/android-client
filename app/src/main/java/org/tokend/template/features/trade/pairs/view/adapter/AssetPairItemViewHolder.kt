package org.tokend.template.features.trade.pairs.view.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.jetbrains.anko.find
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.CircleLogoUtil
import org.tokend.template.view.util.formatter.AmountFormatter

class AssetPairItemViewHolder(
        view: View,
        private val amountFormatter: AmountFormatter
) : BaseViewHolder<AssetPairListItem>(view) {
    private val priceTextView = view.find<TextView>(R.id.price_text_view)
    private val baseCodeTextView = view.find<TextView>(R.id.base_asset_code_text_view)
    private val restCodeTextView = view.find<TextView>(R.id.rest_pair_code_text_view)
    private val baseLogoImageView = view.find<ImageView>(R.id.base_asset_logo_image_view)
    private val dividerView = view.findViewById<View>(R.id.divider_view)

    var dividerIsVisible: Boolean
        get() = dividerView.visibility == View.VISIBLE
        set(value) {
            dividerView.visibility = if (value) View.VISIBLE else View.GONE
        }

    override fun bind(item: AssetPairListItem) {
        baseCodeTextView.text = item.baseAsset.code
        restCodeTextView.text = view.context.getString(
                R.string.template_asset_pair,
                "",
                item.quoteAsset.code
        )

        priceTextView.text = amountFormatter.formatAssetAmount(
                item.price,
                item.quoteAsset,
                withAssetCode = false,
                minDecimalDigits = item.quoteAsset.trailingDigits
        )

        CircleLogoUtil.setLogo(baseLogoImageView, item.baseAsset.code, item.baseAssetLogoUrl)
    }
}