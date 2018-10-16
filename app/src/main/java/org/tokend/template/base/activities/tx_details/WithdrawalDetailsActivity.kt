package org.tokend.template.base.activities.tx_details

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.sdk.api.base.model.transactions.WithdrawalTransaction
import org.tokend.template.R
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter

class WithdrawalDetailsActivity : TxDetailsActivity<WithdrawalTransaction>(WithdrawalTransaction::class) {
    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.withdrawal_details_title)
    }

    override fun displayDetails(item: WithdrawalTransaction) {
        displayStateIfNeeded(item, cards_layout)
        displayDestination(item)
        displayPaid(item)
        displaySent(item)
        displayDate(item, cards_layout)
    }

    private fun displayDestination(tx: WithdrawalTransaction) {
        InfoCard(cards_layout)
                .setHeading(R.string.tx_withdrawal_destination, null)
                .addRow(tx.destAddress ?: getString(R.string.unknown), null)
    }

    private fun displayPaid(tx: WithdrawalTransaction) {
        val paid = tx.amount + tx.fee.total

        InfoCard(cards_layout)
                .setHeading(R.string.paid,
                        "${AmountFormatter.formatAssetAmount(paid)} ${tx.asset}")
                .addRow(R.string.amount_sent,
                        "+${AmountFormatter.formatAssetAmount(tx.amount,
                                minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                        } ${tx.asset}")
                .addRow(R.string.tx_fixed_fee,
                        "+${AmountFormatter.formatAssetAmount(tx.fee.fixed,
                                minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                        } ${tx.asset}")
                .addRow(R.string.tx_percent_fee,
                        "+${AmountFormatter.formatAssetAmount(tx.fee.percent,
                                minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                        } ${tx.asset}")
    }

    private fun displaySent(tx: WithdrawalTransaction) {
        InfoCard(cards_layout)
                .setHeading(R.string.sent,
                        "${AmountFormatter.formatAssetAmount(tx.destAmount)} " +
                                tx.asset)
                .addRow(getString(R.string.template_withdrawal_fee_warning, tx.asset),
                        null)
    }
}