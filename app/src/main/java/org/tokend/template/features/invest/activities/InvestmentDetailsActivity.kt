package org.tokend.template.features.invest.activities

import org.tokend.sdk.api.base.model.operations.InvestmentOperation
import org.tokend.sdk.api.base.model.operations.OfferMatchOperation
import org.tokend.template.R
import org.tokend.template.base.activities.tx_details.OfferMatchDetailsActivity

class InvestmentDetailsActivity : OfferMatchDetailsActivity(
        InvestmentOperation::class
) {
    override fun displayDetails(item: OfferMatchOperation) {
        super.displayDetails(item)
        if (isPending) {
            setTitle(R.string.pending_investment_details_title)
        }
    }

    override fun displayReceived(tx: OfferMatchOperation) {
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