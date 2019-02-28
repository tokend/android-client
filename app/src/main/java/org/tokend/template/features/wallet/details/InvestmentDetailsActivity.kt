package org.tokend.template.features.wallet.details

import org.tokend.template.R

class InvestmentDetailsActivity : OfferMatchDetailsActivity() {
    override fun getTitleString(): String {
        return getString(R.string.investment_details_title)
    }
}