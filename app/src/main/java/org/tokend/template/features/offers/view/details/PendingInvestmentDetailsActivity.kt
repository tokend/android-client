package org.tokend.template.features.offers.view.details

import android.support.v4.content.ContextCompat
import org.tokend.template.R
import org.tokend.template.features.offers.model.OfferRecord
import org.tokend.template.view.details.DetailsItem

class PendingInvestmentDetailsActivity : PendingOfferDetailsActivity() {
    override fun displayDetails(item: OfferRecord) {
        displayToken(item)
        displayDate(item)
        displayToPay(item)
    }

    private fun displayToken(item: OfferRecord) {
        adapter.addData(
                DetailsItem(
                        text = item.baseAssetCode,
                        hint = getString(R.string.sale_asset),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_token)
                )
        )
    }

    override fun getOfferCancellationMessage(): String {
        return getString(R.string.cancel_investment_confirmation)
    }

    override fun getOfferCanceledMessage(): String {
        return getString(R.string.investment_canceled)
    }
}