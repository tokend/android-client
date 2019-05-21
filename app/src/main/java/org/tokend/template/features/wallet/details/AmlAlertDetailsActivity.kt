package org.tokend.template.features.wallet.details

import android.support.v4.content.ContextCompat
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.details.DetailsItem

class AmlAlertDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        val details = item.cause as? BalanceChangeCause.AmlAlert

        if (details == null) {
            finish()
            return
        }

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