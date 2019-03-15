package org.tokend.template.features.wallet.details

import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.BalanceChangeAction
import org.tokend.template.view.InfoCard
import org.tokend.template.view.util.LocalizedName
import java.math.BigDecimal

class UnknownDetailsActivity : BalanceChangeDetailsActivity() {

    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.unknown_balance_change_details_title)

        displayEffect(item)
        displayBalanceChange(item)
        displayDate(item, cards_layout)
    }

    private fun displayEffect(item: BalanceChange) {
        InfoCard(cards_layout)
                .setHeading(R.string.tx_effect, null)
                .addRow(LocalizedName(this).forBalanceChangeAction(item.action), null)
    }

    private fun displayBalanceChange(item: BalanceChange) {
        val minDigits = amountFormatter.getDecimalDigitsCount(item.assetCode)
        val asset = item.assetCode

        val titleRes: Int
        val total: BigDecimal

        val amountString = amountFormatter.formatAssetAmount(item.amount, asset, minDigits)
        var fixedFeeString = amountFormatter.formatAssetAmount(item.fee.fixed, asset, minDigits)
        var percentFeeString = amountFormatter.formatAssetAmount(item.fee.percent, asset, minDigits)

        if (item.isReceived != null) {
            titleRes = if (item.isReceived) {
                R.string.received
            } else {
                R.string.charged
            }

            // Unlocked action is 'received' but fees are received as well as the amount.
            if (item.isReceived && item.action != BalanceChangeAction.UNLOCKED) {
                total = item.amount - item.fee.total
                fixedFeeString = "-$fixedFeeString"
                percentFeeString = "-$percentFeeString"
            } else {
                total = item.amount + item.fee.total
                fixedFeeString = "+$fixedFeeString"
                percentFeeString = "+$percentFeeString"
            }

            InfoCard(cards_layout)
                    .setHeading(titleRes,
                            amountFormatter.formatAssetAmount(total, item.assetCode, minDigits))
                    .addRow(R.string.amount, "+$amountString")
                    .addRow(R.string.fixed_fee, fixedFeeString)
                    .addRow(R.string.percent_fee, percentFeeString)
        } else {
            InfoCard(cards_layout)
                    .setHeading(R.string.amount, amountString)
                    .addRow(R.string.fixed_fee, fixedFeeString)
                    .addRow(R.string.percent_fee, percentFeeString)
        }
    }
}
