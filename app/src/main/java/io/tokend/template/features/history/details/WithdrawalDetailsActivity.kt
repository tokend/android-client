package io.tokend.template.features.history.details

import androidx.core.content.ContextCompat
import io.tokend.template.R
import io.tokend.template.features.history.model.BalanceChange
import io.tokend.template.features.history.model.details.BalanceChangeCause
import io.tokend.template.view.details.DetailsItem

class WithdrawalDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        super.displayDetails(item)

        val details = item.cause as? BalanceChangeCause.WithdrawalRequest

        if (details == null) {
            finishWithError(IllegalStateException("Invalid item cause type"))
            return
        }

        displayDestination(details)
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