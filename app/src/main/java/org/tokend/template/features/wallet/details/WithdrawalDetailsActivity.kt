package org.tokend.template.features.wallet.details

import android.support.v4.content.ContextCompat
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.details.DetailsItem

class WithdrawalDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        val details = item.cause as? BalanceChangeCause.WithdrawalRequest

        if (details == null) {
            finish()
            return
        }

        displayEffect(item, adapter)
        displayBalanceChange(item, adapter)
        displayDestination(details)
        displayDate(item, adapter)
    }

    private fun displayDestination(cause: BalanceChangeCause.WithdrawalRequest) {
        adapter.addData(
                DetailsItem(
                        text = cause.destinationAddress,
                        hint = getString(R.string.tx_withdrawal_destination),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_forward)
                )
        )
    }
}