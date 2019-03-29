package org.tokend.template.features.trade.pairs.view.adapter

import android.view.View
import android.widget.TextView
import org.jetbrains.anko.find
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.formatter.AmountFormatter

class AssetPairItemViewHolder(
        view: View,
        private val amountFormatter: AmountFormatter
) : BaseViewHolder<AssetPairListItem>(view) {
    private val priceTextView = view.find<TextView>(R.id.price_text_view)
    private val codeTextView = view.find<TextView>(R.id.code_text_view)
    private val dividerView = view.find<View>(R.id.divider_view)

    override fun bind(item: AssetPairListItem) {
        codeTextView.text = view.context.getString(
                R.string.template_asset_pair,
                item.baseAssetCode,
                item.quoteAssetCode
        )

        priceTextView.text = amountFormatter.formatAssetAmount(
                item.price,
                item.quoteAssetCode,
                withAssetCode = false,
                minDecimalDigits = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS
        )
    }

    var dividerIsVisible: Boolean = true
        set(value) {
            field = value
            dividerView.visibility =
                    if (field)
                        View.VISIBLE
                    else
                        View.GONE
        }
}