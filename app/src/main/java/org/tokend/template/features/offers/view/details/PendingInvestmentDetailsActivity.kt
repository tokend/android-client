package org.tokend.template.features.offers.view.details

import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.view.InfoCard

class PendingInvestmentDetailsActivity: PendingOfferDetailsActivity() {
    override fun getTitleString(): String {
        return getString(R.string.pending_investment_details_title)
    }

    override fun displayDetails(item: OfferRecord) {
        displayToken(item)
        displayToPay(item)
        displayDate(item)
    }

    private fun displayToken(item: OfferRecord) {
        InfoCard(cards_layout)
                .setHeading(R.string.sale_token, null)
                .addRow(item.baseAssetCode, null)
    }

    override fun getOfferCancellationMessage(): String {
        return getString(R.string.cancel_investment_confirmation)
    }

    override fun getOfferCanceledMessage(): String {
        return getString(R.string.investment_canceled)
    }
}