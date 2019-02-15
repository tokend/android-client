package org.tokend.template.features.wallet.details

import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.InfoCard

class IssuanceDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.issuance_details_title)

        val details = item.cause as? BalanceChangeCause.Issuance

        if (details == null) {
            finish()
            return
        }

        displayReceived(item, details)
        displayReference(details)
        displayDate(item, cards_layout)
    }

    private fun displayReceived(item: BalanceChange,
                                cause: BalanceChangeCause.Issuance) {
        InfoCard(cards_layout)
                .setHeading(R.string.received, null)
                .addRow(R.string.amount,
                        amountFormatter.formatAssetAmount(item.amount, item.assetCode))
                .apply {
                    val cause = cause.cause

                    if (cause != null) {
                        addRow(R.string.tx_cause, cause)
                    }
                }
    }

    private fun displayReference(cause: BalanceChangeCause.Issuance) {
        val reference = cause.reference?.takeIf { it.isNotBlank() }
                ?: return

        InfoCard(cards_layout)
                .setHeading(R.string.tx_reference, null)
                .addRow(reference, null)
    }
}