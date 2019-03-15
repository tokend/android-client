package org.tokend.template.features.wallet.details

import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.view.InfoCard

class SaleCancellationDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.sale_cancellation_details_title)

        displayUnlocked(item)
        displayDate(item, cards_layout)
    }

    private fun displayUnlocked(item: BalanceChange) {
        InfoCard(cards_layout)
                .setHeading(R.string.unlocked, null)
                .addRow(R.string.amount,
                        amountFormatter.formatAssetAmount(item.amount, item.assetCode))
    }
}