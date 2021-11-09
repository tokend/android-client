package io.tokend.template.features.invest.view

import android.os.Bundle
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import io.tokend.template.R
import io.tokend.template.extensions.getNullableStringExtra
import io.tokend.template.features.offers.OfferConfirmationActivity
import io.tokend.template.features.offers.model.OfferRequest
import io.tokend.template.view.details.DetailsItem
import kotlinx.android.synthetic.main.layout_balance_change_main_data.*
import org.tokend.wallet.xdr.FeeType

class InvestmentConfirmationActivity : OfferConfirmationActivity() {
    private var saleName: String? = null
    private var displayToReceive = false

    override val feeType: Int
        get() = FeeType.INVEST_FEE.value

    override fun displayDetails() {
        saleName = intent.getNullableStringExtra(SALE_NAME_EXTRA)
        displayToReceive = intent.getBooleanExtra(DISPLAY_TO_RECEIVE, false)

        (top_info_text_view.layoutParams as? LinearLayout.LayoutParams)?.also {
            it.topMargin = 0
            top_info_text_view.layoutParams = it
        }

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
        private const val SALE_NAME_EXTRA = "sale_name"
        private const val DISPLAY_TO_RECEIVE = "display_to_receive"

        fun getBundle(
            offerRequest: OfferRequest,
            displayToReceive: Boolean,
            saleName: String?
        ) = Bundle().apply {
            putBoolean(DISPLAY_TO_RECEIVE, displayToReceive)
            putString(SALE_NAME_EXTRA, saleName)
            putAll(getBundle(offerRequest))
        }
    }
}