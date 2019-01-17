package org.tokend.template.features.wallet.txdetails

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.sdk.api.base.model.operations.WithdrawalOperation
import org.tokend.template.R
import org.tokend.template.view.InfoCard

class WithdrawalDetailsActivity : TxDetailsActivity<WithdrawalOperation>(WithdrawalOperation::class) {
    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.withdrawal_details_title)
    }

    override fun displayDetails(item: WithdrawalOperation) {
        displayStateIfNeeded(item, cards_layout)
        displayDestination(item)
        displayPaid(item)
        displaySent(item)
        displayDate(item, cards_layout)
    }

    private fun displayDestination(tx: WithdrawalOperation) {
        InfoCard(cards_layout)
                .setHeading(R.string.tx_withdrawal_destination, null)
                .addRow(tx.destAddress ?: getString(R.string.unknown), null)
    }

    private fun displayPaid(tx: WithdrawalOperation) {
        val paid = tx.amount + tx.fee.total

        InfoCard(cards_layout)
                .setHeading(R.string.paid,
                        "${amountFormatter.formatAssetAmount(paid, tx.asset)} ${tx.asset}")
                .addRow(R.string.amount_sent,
                        "+${amountFormatter.formatAssetAmount(tx.amount, tx.asset)
                        } ${tx.asset}")
                .addRow(R.string.tx_fixed_fee,
                        "+${amountFormatter.formatAssetAmount(tx.fee.fixed, tx.asset)
                        } ${tx.asset}")
                .addRow(R.string.tx_percent_fee,
                        "+${amountFormatter.formatAssetAmount(tx.fee.percent, tx.asset)
                        } ${tx.asset}")
    }

    private fun displaySent(tx: WithdrawalOperation) {
        InfoCard(cards_layout)
                .setHeading(R.string.sent,
                        "${amountFormatter.formatAssetAmount(tx.destAmount, tx.asset)} " +
                                tx.asset)
                .addRow(getString(R.string.template_withdrawal_fee_warning, tx.asset),
                        null)
    }
}