package io.tokend.template.features.dashboard.balances.view.adapter

import android.text.SpannableString
import android.view.View
import androidx.core.content.ContextCompat
import io.tokend.template.R
import io.tokend.template.extensions.highlight
import io.tokend.template.extensions.setFontSize
import io.tokend.template.view.adapter.base.BaseViewHolder
import io.tokend.template.view.balances.BalanceItemView
import io.tokend.template.view.balances.BalanceItemViewImpl
import io.tokend.template.view.util.formatter.AmountFormatter

class BalanceItemViewHolder(
    view: View,
    private val amountFormatter: AmountFormatter
) : BaseViewHolder<BalanceListItem>(view), BalanceItemView by BalanceItemViewImpl(view) {
    private val secondaryTextColor: Int =
        ContextCompat.getColor(view.context, R.color.secondary_text)
    private val hintTextSize: Int =
        view.context.resources.getDimensionPixelSize(R.dimen.text_size_hint)

    override fun bind(item: BalanceListItem) {
        displayLogo(item.logoUrl, item.asset.code)

        nameTextView.text = item.displayedName

        val conversionAsset = item.conversionAsset

        if (conversionAsset == null || item.asset == conversionAsset
            || item.converted == null
        ) {
            amountTextView.text = amountFormatter.formatAssetAmount(
                item.available,
                item.asset
            )
        } else {
            val availableString = amountFormatter.formatAssetAmount(
                item.available,
                item.asset
            )

            val balanceString = view.context.getString(
                R.string.template_amount_with_converted,
                availableString,
                amountFormatter.formatAssetAmount(
                    item.converted,
                    item.conversionAsset
                )
            )

            val spannableString = SpannableString(balanceString)
            val substringToHighlight = balanceString.substring(availableString.length)
            spannableString.highlight(substringToHighlight, secondaryTextColor)
            spannableString.setFontSize(substringToHighlight, hintTextSize)

            amountTextView.text = spannableString
        }
    }
}