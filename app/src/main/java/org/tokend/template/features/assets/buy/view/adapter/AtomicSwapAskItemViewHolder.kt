package org.tokend.template.features.assets.buy.view.adapter

import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.apmem.tools.layouts.FlowLayout
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.formatter.AmountFormatter

class AtomicSwapAskItemViewHolder(view: View,
                                  private val amountFormatter: AmountFormatter
) : BaseViewHolder<AtomicSwapAskListItem>(view) {
    private val availableTextView: TextView = view.findViewById(R.id.available_text_view)
    private val pricesHintTextView: TextView = view.findViewById(R.id.prices_hint_text_view)
    private val pricesLayout: ViewGroup = view.findViewById(R.id.prices_layout)
    private val priceThemedContext = ContextThemeWrapper(view.context, R.style.StrokedBadgeText)

    private val priceTextViewMargin =
            view.context.resources.getDimensionPixelSize(R.dimen.quarter_standard_margin)

    override fun bind(item: AtomicSwapAskListItem) {
        availableTextView.text = amountFormatter.formatAssetAmount(
                item.available,
                item.asset,
                withAssetCode = false
        )

        pricesHintTextView.text = view.context.getString(
                R.string.template_with_one_asset_for,
                item.asset.code
        )

        pricesLayout.removeAllViews()
        item.quoteAssets.forEach { quoteAsset ->
            val textView = TextView(priceThemedContext, null, R.style.StrokedBadgeText)
            textView.text = amountFormatter.formatAssetAmount(
                    quoteAsset.price,
                    quoteAsset
            )
            pricesLayout.addView(textView)
            textView.layoutParams = (textView.layoutParams as FlowLayout.LayoutParams).apply {
                setMargins(priceTextViewMargin, priceTextViewMargin, priceTextViewMargin, priceTextViewMargin)
            }
        }
    }
}