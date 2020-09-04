package org.tokend.template.features.history.details

import androidx.core.content.ContextCompat
import org.tokend.template.R
import org.tokend.template.features.history.model.BalanceChange
import org.tokend.template.features.history.model.details.BalanceChangeCause
import org.tokend.template.view.details.DetailsItem

class AmlAlertDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        super.displayDetails(item)

        val details = item.cause as? BalanceChangeCause.AmlAlert

        if (details == null) {
            finishWithError(IllegalStateException("Invalid item cause type"))
            return
        }

        displayReason(details)
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