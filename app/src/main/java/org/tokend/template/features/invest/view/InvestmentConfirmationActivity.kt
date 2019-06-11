package org.tokend.template.features.invest.view

import android.support.v4.content.ContextCompat
import org.tokend.template.R
import org.tokend.template.extensions.getNullableStringExtra
import org.tokend.template.features.offers.OfferConfirmationActivity
import org.tokend.template.view.details.DetailsItem
import org.tokend.wallet.xdr.FeeType

class InvestmentConfirmationActivity : OfferConfirmationActivity() {
    private var saleName: String? = null
    private var displayToReceive = false

    override val feeType: Int
        get() = FeeType.INVEST_FEE.value

    override fun displayDetails() {
        saleName = intent.getNullableStringExtra(SALE_NAME_EXTRA)
        displayToReceive = intent.getBooleanExtra(DISPLAY_TO_RECEIVE, false)

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
        val titleId = if (!cancellationOnly) {
            if (offerToCancel != null)
                R.string.investment_updating_title
            else
                R.string.investment_confirmation_title
        } else {
           R.string.investment_cancellation_title
        }
        mainDataView.displayOperationName(getString(titleId))
    }

    override fun getSuccessMessage(): String {
        return if (!cancellationOnly) {
            if (offerToCancel != null)
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