package org.tokend.template.features.wallet.details

import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_details_list.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.details.DetailsItem
import org.tokend.template.view.details.adapter.DetailsItemsAdapter

class AmlAlertDetailsActivity : BalanceChangeDetailsActivity() {
    private val adapter = DetailsItemsAdapter()

    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details_list)

        val details = item.cause as? BalanceChangeCause.AmlAlert

        if (details == null) {
            finish()
            return
        }

        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter

        displayEffect(item, adapter)
        displayReason(details)
        displayDate(item, adapter)
    }

    private fun displayReason(cause: BalanceChangeCause.AmlAlert) {
        val reason = cause.reason?.takeIf { it.isNotBlank() }
                ?: getString(R.string.aml_alert_reason_unknown)

        adapter.addData(
                DetailsItem(
                        text = reason,
                        hint = getString(R.string.aml_alert_reason),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_label_outline)
                )
        )
    }
}