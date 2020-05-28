package org.tokend.template.features.offers.view.details

import androidx.core.content.ContextCompat
import org.tokend.template.R
import org.tokend.template.features.offers.model.OfferRecord
import org.tokend.template.view.details.DetailsItem

class PendingInvestmentDetailsActivity : PendingOfferDetailsActivity() {
    override fun displayDetails(item: OfferRecord) {
        displayToken(item)
        super.displayDetails(item)
    }

    override fun displayPrice(item: OfferRecord) {}

    override fun displayToReceive(item: OfferRecord) {}

    private fun displayToken(item: OfferRecord) {
        adapter.addData(
                DetailsItem(
                        text = item.baseAsset.code,
                        hint = getString(R.string.sale_asset),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_token)
                )
        )
    }

    override fun displayOperationName() {
        mainDataView.displayOperationName(getString(R.string.balance_change_cause_pending_investment))
    }

    override fun getOfferCancellationMessage(): String {
        return getString(R.string.cancel_investment_confirmation)
    }

    override fun getOfferCanceledMessage(): String {
        return getString(R.string.investment_canceled)
    }
}