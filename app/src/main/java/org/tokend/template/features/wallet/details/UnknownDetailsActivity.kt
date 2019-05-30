package org.tokend.template.features.wallet.details

import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange

open class UnknownDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        super.displayDetails(item)
        displayOperationName(getString(R.string.unknown_balance_change))
    }
}
