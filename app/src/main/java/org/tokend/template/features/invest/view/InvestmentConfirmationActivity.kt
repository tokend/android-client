package org.tokend.template.features.invest.view

import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.extensions.getNullableStringExtra
import org.tokend.template.features.offers.OfferConfirmationActivity
import org.tokend.template.view.InfoCard

class InvestmentConfirmationActivity : OfferConfirmationActivity() {
    private var assetName: String? = null
    private var displayToReceive = false

    override fun initData() {
        super.initData()
        assetName = intent.getNullableStringExtra(ASSET_NAME_EXTRA)
        displayToReceive = intent.getBooleanExtra(DISPLAY_TO_RECEIVE, false)
    }

    override fun displayDetails() {
        cards_layout.removeAllViews()

        displayToken()
        displayToPay()

        if (displayToReceive) {
            displayToReceive()
        }

        displayTitle()
    }

    private fun displayToken() {
        InfoCard(cards_layout)
                .setHeading(R.string.sale_asset, null)
                .addRow(assetName, offer.baseAssetCode)
    }

    private fun displayTitle() {
        if (!cancellationOnly) {
            if (prevOffer != null)
                setTitle(R.string.investment_updating_title)
            else
                setTitle(R.string.investment_confirmation_title)
        } else {
            setTitle(R.string.investment_cancellation_title)
        }
    }

    override fun getSuccessMessage(): String {
        return if (!cancellationOnly) {
            if (prevOffer != null)
                getString(R.string.investment_updated)
            else
                getString(R.string.successfully_invested)
        } else {
            getString(R.string.investment_canceled)
        }
    }

    companion object {
        const val ASSET_NAME_EXTRA = "asset_name"
        const val DISPLAY_TO_RECEIVE = "display_to_receive"
    }
}