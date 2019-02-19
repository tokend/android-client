package org.tokend.template.features.wallet.details

import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.InfoCard

class WithdrawalDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.withdrawal_details_title)

        val details = item.cause as? BalanceChangeCause.Withdrawal

        if (details == null) {
            finish()
            return
        }

        displayPaid(item)
        displayDestination(item, details)
        displayDate(item, cards_layout)
    }

    private fun displayPaid(item: BalanceChange) {
        val totalPaid = item.amount + item.fee.total
        val minDecimals = amountFormatter.getDecimalDigitsCount(item.assetCode)

        InfoCard(cards_layout)
                .setHeading(R.string.paid,
                        amountFormatter.formatAssetAmount(totalPaid, item.assetCode, minDecimals))
                .addRow(R.string.amount,
                        "+" + amountFormatter.formatAssetAmount(totalPaid, item.assetCode,
                                minDecimals))
                .addRow(R.string.fixed_fee,
                        "+" + amountFormatter.formatAssetAmount(item.fee.fixed, item.assetCode,
                                minDecimals))
                .addRow(R.string.percent_fee,
                        "+" + amountFormatter.formatAssetAmount(item.fee.percent, item.assetCode,
                                minDecimals))
    }

    private fun displayDestination(item: BalanceChange,
                                   cause: BalanceChangeCause.Withdrawal) {
        InfoCard(cards_layout)
                .setHeading(R.string.tx_withdrawal_destination, null)
                .addRow(cause.destinationAddress, null)
                .addRow("\n" + getString(R.string.template_withdrawal_fee_warning, item.assetCode), null)
    }
}