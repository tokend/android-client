package org.tokend.template.base.activities.tx_details

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.sdk.api.models.transactions.InvestmentTransaction
import org.tokend.template.R
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter

class InvestmentDetailsActivity : TxDetailsActivity<InvestmentTransaction>(InvestmentTransaction::class) {
    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.investment_details_title)
    }

    override fun displayDetails(item: InvestmentTransaction) {
        displayStateIfNeeded(item, cards_layout)
        displayInvested(item)
        displayReceived(item)
        displayDate(item, cards_layout)
    }

    private fun displayInvested(tx: InvestmentTransaction) {
        val investedAmount =
                if (tx.isReceived) tx.matchData.quoteAmount else tx.amount
        val investedAsset = if (tx.isReceived) tx.matchData.quoteAsset else tx.asset

        InfoCard(cards_layout)
                .setHeading(R.string.invested, "${AmountFormatter.formatAssetAmount(investedAmount)} " +
                        investedAsset)
                .addRow(R.string.tx_fee, "${AmountFormatter.formatAssetAmount(tx.fee)
                } $investedAsset")
    }

    private fun displayReceived(tx: InvestmentTransaction) {
        val receivedAmount =
                if (tx.isReceived) tx.amount else tx.matchData.quoteAmount
        val receivedAsset = if (tx.isReceived) tx.asset else tx.matchData.quoteAsset
        val investedAsset = if (tx.isReceived) tx.matchData.quoteAsset else tx.asset

        InfoCard(cards_layout)
                .setHeading(R.string.received,
                        "${AmountFormatter.formatAssetAmount(receivedAmount, receivedAsset)
                        } $receivedAsset")
                .addRow(R.string.price, getString(R.string.template_price_one_for, receivedAsset,
                        AmountFormatter.formatAssetAmount(tx.matchData.price), investedAsset))
    }
}