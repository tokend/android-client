package org.tokend.template.features.wallet.txdetails

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.sdk.api.base.model.operations.IssuanceOperation
import org.tokend.template.R
import org.tokend.template.view.InfoCard
import org.tokend.template.view.util.formatter.AmountFormatter

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
                .addRow(R.string.amount,
                amountFormatter.formatAssetAmount(tx.amount, tx.asset,
                        minDecimalDigits = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS))
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