package org.tokend.template.base.activities.tx_details

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.sdk.api.models.transactions.MatchTransaction
import org.tokend.template.R
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter

class OfferMatchDetailsActivity : TxDetailsActivity<MatchTransaction>() {
    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.offer_match_details_title)
    }

    override fun displayDetails(item: MatchTransaction) {
        displayStateIfNeeded(item, cards_layout)
        displayPaid(item)
        displayReceived(item)
        displayDate(item, cards_layout)
    }

    private fun displayPaid(tx: MatchTransaction) {
        val isIncome = !tx.isSent(walletInfoProvider.getWalletInfo()?.accountId ?: "")

        val paidAmount =
                if (isIncome) tx.matchData.quoteAmount else tx.amount
        val paidAsset = if (isIncome) tx.matchData.quoteAsset else tx.asset

        InfoCard(cards_layout)
                .setHeading(R.string.paid, "${AmountFormatter.formatAssetAmount(paidAmount)} " +
                        paidAsset)
                .addRow(R.string.tx_fee, "${AmountFormatter.formatAssetAmount(tx.fee)
                } $paidAsset")
    }

    private fun displayReceived(tx: MatchTransaction) {
        val isIncome = !tx.isSent(walletInfoProvider.getWalletInfo()?.accountId ?: "")

        val receivedAmount =
                if (isIncome) tx.amount else tx.matchData.quoteAmount
        val receivedAsset = if (isIncome) tx.asset else tx.matchData.quoteAsset
        val paidAsset = if (isIncome) tx.matchData.quoteAsset else tx.asset

        InfoCard(cards_layout)
                .setHeading(R.string.received,
                        "${AmountFormatter.formatAssetAmount(receivedAmount, receivedAsset)
                        } $receivedAsset")
                .addRow(R.string.price, getString(R.string.template_price_one_for, receivedAsset,
                        AmountFormatter.formatAssetAmount(tx.matchData.price), paidAsset))
    }
}