package org.tokend.template.features.offers.view.details

import org.tokend.template.R
import org.tokend.template.data.model.OfferRecord

class PendingInvestmentDetailsActivity: PendingOfferDetailsActivity() {
    override fun getTitleString(): String {
        return getString(R.string.pending_investment_details_title)
    }

    override fun displayToReceive(item: OfferRecord) {
        // For pending investments receive amount is unknown
    }

    override fun displayPrice(item: OfferRecord) {
        // For pending investments price is unknown
    }

    override fun getOfferCancellationMessage(): String {
        return getString(R.string.cancel_investment_confirmation)
    }

    override fun getOfferCanceledMessage(): String {
        return getString(R.string.investment_canceled)
    }
}