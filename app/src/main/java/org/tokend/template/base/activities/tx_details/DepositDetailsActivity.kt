package org.tokend.template.base.activities.tx_details

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.sdk.api.base.model.operations.IssuanceOperation
import org.tokend.template.R
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter

class DepositDetailsActivity :
        TxDetailsActivity<IssuanceOperation>(IssuanceOperation::class) {
    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.deposit_details_title)
    }

    override fun displayDetails(item: IssuanceOperation) {
        displayStateIfNeeded(item, cards_layout)
        displayAmountAndCause(item)
        displayReferenceIfNeeded(item)
        displayDate(item, cards_layout)
    }

    private fun displayAmountAndCause(tx: IssuanceOperation) {
        InfoCard(cards_layout)
                .setHeading(R.string.received, null)
                .addRow(R.string.amount,"${
                AmountFormatter.formatAssetAmount(tx.amount,
                        minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                } ${tx.asset}")
                .apply {
                    if (!tx.cause.isNullOrBlank()) {
                        addRow(R.string.tx_cause, tx.cause)
                    }
                }
    }

    private fun displayReferenceIfNeeded(tx: IssuanceOperation) {
        val reference = tx.reference.takeIf { !it.isNullOrBlank() } ?: return

        InfoCard(cards_layout)
                .setHeading(R.string.tx_reference, null)
                .addRow(reference, null)
    }
}