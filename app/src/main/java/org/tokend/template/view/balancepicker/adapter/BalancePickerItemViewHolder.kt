package org.tokend.template.view.balancepicker.adapter

import android.support.v4.content.ContextCompat
import android.view.View
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.balances.BalanceItemView
import org.tokend.template.view.balances.BalanceItemViewImpl
import org.tokend.template.view.util.formatter.AmountFormatter

class BalancePickerItemViewHolder(
        view: View,
        private val amountFormatter: AmountFormatter
) : BaseViewHolder<BalancePickerListItem>(view), BalanceItemView by BalanceItemViewImpl(view) {
    private val colorDefaultText = ContextCompat.getColor(view.context, R.color.primary_text)
    private val colorSecondaryText = ContextCompat.getColor(view.context, R.color.secondary_text)
    private val colorError = ContextCompat.getColor(view.context, R.color.error)

    override fun bind(item: BalancePickerListItem) {
        displayLogo(item.logoUrl, item.assetCode)

        nameTextView.text = item.assetCode

        if (item.available != null) {
            amountTextView.text = view.context.getString(
                    R.string.template_available,
                    amountFormatter.formatAssetAmount(
                            item.available,
                            item.assetCode,
                            withAssetCode = false
                    )
            )
            amountTextView.visibility = View.VISIBLE
        } else {
            amountTextView.visibility = View.GONE
        }

        if (item.isEnough) {
            amountTextView.setTextColor(colorDefaultText)
            nameTextView.setTextColor(colorDefaultText)
            logoImageView.alpha = 1f
            amountTextView.alpha = 1f
        } else {
            amountTextView.setTextColor(colorError)
            nameTextView.setTextColor(colorSecondaryText)
            logoImageView.alpha = DISABLED_ALPHA
            amountTextView.alpha = DISABLED_ALPHA
        }
    }

    companion object {
        private const val DISABLED_ALPHA = 0.6f
    }
}