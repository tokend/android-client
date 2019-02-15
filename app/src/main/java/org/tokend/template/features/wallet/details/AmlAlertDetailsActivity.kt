package org.tokend.template.features.wallet.details

import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.InfoCard

class AmlAlertDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.aml_alert_details_title)

        val details = item.cause as? BalanceChangeCause.AmlAlert

        if (details == null) {
            finish()
            return
        }

        displayReason(details)
        displayDate(item, cards_layout)
    }

    private fun displayReason(cause: BalanceChangeCause.AmlAlert) {
        val reason = cause.reason?.takeIf { it.isNotBlank() }
                ?: getString(R.string.aml_alert_reason_unknown)

        InfoCard(cards_layout)
                .setHeading(R.string.aml_alert_reason, null)
                .addRow(reason, null)
    }
}