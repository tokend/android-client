package org.tokend.template.base.activities.tx_details

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.sdk.api.models.transactions.BaseTransaction
import org.tokend.template.R
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter

class UnknownTxDetailsActivity : TxDetailsActivity<BaseTransaction>(BaseTransaction::class) {

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.transaction_details_title)
    }

    override fun displayDetails(item: BaseTransaction) {
        displayStateIfNeeded(item, cards_layout)
        displayAmount(item)
        displayDate(item, cards_layout)
    }

    private fun displayAmount(item: BaseTransaction) {
        InfoCard(cards_layout)
                .setHeading(if (item.isSent) R.string.paid else R.string.received, null)
                .addRow("${
                AmountFormatter.formatAssetAmount(item.amount,
                        minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                } ${item.asset}", null)
    }
}