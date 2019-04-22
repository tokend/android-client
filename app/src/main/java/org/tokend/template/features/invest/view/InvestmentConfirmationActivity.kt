package org.tokend.template.features.invest.view

import android.support.v4.content.ContextCompat
import org.tokend.template.R
import org.tokend.template.extensions.getNullableStringExtra
import org.tokend.template.features.offers.OfferConfirmationActivity
import org.tokend.template.view.details.DetailsItem

class InvestmentConfirmationActivity : OfferConfirmationActivity() {
    private var saleName: String? = null
    private var displayToReceive = false

    override fun initData() {
        super.initData()
        saleName = intent.getNullableStringExtra(SALE_NAME_EXTRA)
        displayToReceive = intent.getBooleanExtra(DISPLAY_TO_RECEIVE, false)
    }

    override fun displayDetails() {
        displaySale()
        displayToPay()

        if (displayToReceive) {
            displayToReceive()
        }

        displayTitle()
    }

    private fun displaySale() {
        val name = saleName ?: return

        adapter.addData(
                DetailsItem(
                        text = name,
                        hint = getString(R.string.sale_title),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_invest)
                )
        )
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
        const val SALE_NAME_EXTRA = "sale_name"
        const val DISPLAY_TO_RECEIVE = "display_to_receive"
    }
}