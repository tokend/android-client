package org.tokend.template.features.invest.activities

import org.tokend.sdk.api.models.transactions.InvestmentTransaction
import org.tokend.sdk.api.models.transactions.MatchTransaction
import org.tokend.template.R
import org.tokend.template.base.activities.tx_details.OfferMatchDetailsActivity

class InvestmentDetailsActivity : OfferMatchDetailsActivity(
        InvestmentTransaction::class
) {
    override fun displayDetails(item: MatchTransaction) {
        super.displayDetails(item)
        if (isPending) {
            setTitle(R.string.pending_investment_details_title)
        }
    }

    override fun displayReceived(tx: MatchTransaction) {
        if (!isPending) {
            super.displayReceived(tx)
        }
    }

    override fun getOfferCancellationMessage(): String {
        return getString(R.string.cancel_investment_confirmation)
    }

    override fun getOfferCanceledMessage(): String {
        return getString(R.string.investment_canceled)
    }
}