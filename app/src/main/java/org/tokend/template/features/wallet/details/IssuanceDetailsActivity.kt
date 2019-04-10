package org.tokend.template.features.wallet.details

import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_details_list.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.details.DetailsItem
import org.tokend.template.view.details.adapter.DetailsItemsAdapter

class IssuanceDetailsActivity : BalanceChangeDetailsActivity() {
    private val adapter = DetailsItemsAdapter()

    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details_list)

        val details = item.cause as? BalanceChangeCause.Issuance

        if (details == null) {
            finish()
            return
        }

        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter

        displayEffect(item, adapter)
        displayBalanceChange(item, adapter)
        displayReferenceAndCause(details)
        displayDate(item, adapter)
    }

    private fun displayReferenceAndCause(details: BalanceChangeCause.Issuance) {
        val reference = details.reference?.takeIf { it.isNotBlank() }
        val cause = details.cause?.takeIf { it.isNotBlank() }

        if (reference != null) {
            adapter.addData(
                    DetailsItem(
                            text = reference,
                            hint = getString(R.string.tx_reference),
                            icon = ContextCompat.getDrawable(this, R.drawable.ic_label_outline)
                    )
            )
        }

        if (cause != null) {
            adapter.addData(
                    DetailsItem(
                            text = cause,
                            hint = getString(R.string.tx_cause),
                            icon =
                            if (reference == null)
                                ContextCompat.getDrawable(this, R.drawable.ic_label_outline)
                            else
                                null
                    )
            )
        }
    }
}