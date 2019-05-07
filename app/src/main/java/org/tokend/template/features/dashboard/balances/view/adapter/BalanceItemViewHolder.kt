package org.tokend.template.features.dashboard.balances.view.adapter

import android.view.View
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.balances.BalanceItemView
import org.tokend.template.view.balances.BalanceItemViewImpl
import org.tokend.template.view.util.formatter.AmountFormatter

class BalanceItemViewHolder(
        view: View,
        private val amountFormatter: AmountFormatter
) : BaseViewHolder<BalanceListItem>(view), BalanceItemView by BalanceItemViewImpl(view) {

    override fun bind(item: BalanceListItem) {
        displayLogo(item.logoUrl, item.assetCode)

        nameTextView.text = item.displayedName

        val conversionAssetCode = item.conversionAssetCode

        amountTextView.text =
                if (conversionAssetCode == null || item.assetCode == conversionAssetCode)
                    amountFormatter.formatAssetAmount(
                            item.available,
                            item.assetCode
                    )
                else
                    view.context.getString(
                            R.string.template_balance_slash_converted,
                            amountFormatter.formatAssetAmount(
                                    item.available,
                                    item.assetCode
                            ),
                            amountFormatter.formatAssetAmount(
                                    item.converted,
                                    item.conversionAssetCode
                            )
                    )
    }
}