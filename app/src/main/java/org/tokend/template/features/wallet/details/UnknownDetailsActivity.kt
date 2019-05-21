package org.tokend.template.features.wallet.details

import org.tokend.template.data.model.history.BalanceChange

open class UnknownDetailsActivity : BalanceChangeDetailsActivity() {

    override fun displayDetails(item: BalanceChange) {
        displayEffect(item, adapter)
        displayBalanceChange(item, adapter)
        displayDate(item, adapter)
    }
}
