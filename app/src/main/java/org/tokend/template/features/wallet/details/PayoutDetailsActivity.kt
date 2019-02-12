package org.tokend.template.features.wallet.details

import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.PayoutDetails
import org.tokend.template.view.InfoCard

class PayoutDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.payout_details_title)

        val details = item.details as? PayoutDetails

        if (details == null) {
            finish()
            return
        }

        val accountId = walletInfoProvider.getWalletInfo()?.accountId

        if (accountId == null) {
            finish()
            return
        }

        if (details.isIssuer(accountId)) {
            displayConditions(details)
            displayResults(details)
        }

        displayReceived(item)
        displayDate(item, cards_layout)
    }

    private fun displayConditions(details: PayoutDetails) {
        InfoCard(cards_layout)
                .setHeading(R.string.payout_conditions, null)
                .addRow(R.string.payout_asset, details.assetCode)
                .addRow(R.string.payout_min_holding_amount,
                        amountFormatter.formatAssetAmount(details.minAssetHolderAmount, details.assetCode))
                .addRow(R.string.payout_min_amount,
                        amountFormatter.formatAssetAmount(details.minPayoutAmount, details.assetCode))
                .addRow(R.string.payout_max_total_amount,
                        amountFormatter.formatAssetAmount(details.maxPayoutAmount, details.assetCode))
    }

    private fun displayResults(details: PayoutDetails) {
        val paidTotal = details.actualPayoutAmount + details.actualFee.total

        InfoCard(cards_layout)
                .setHeading(R.string.paid,
                        amountFormatter.formatAssetAmount(paidTotal, details.assetCode))
                .addRow(R.string.amount,
                        "+" + amountFormatter.formatAssetAmount(details.actualPayoutAmount, details.assetCode))
                .addRow(R.string.fixed_fee,
                        "+" + amountFormatter.formatAssetAmount(details.actualFee.fixed, details.assetCode))
                .addRow(R.string.amount,
                        "+" + amountFormatter.formatAssetAmount(details.actualFee.percent, details.assetCode))
    }

    private fun displayReceived(item: BalanceChange) {
        InfoCard(cards_layout)
                .setHeading(R.string.received, null)
                .addRow(amountFormatter.formatAssetAmount(item.amount, item.assetCode), null)
    }
}