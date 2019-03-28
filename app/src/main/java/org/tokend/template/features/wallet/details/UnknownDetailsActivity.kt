package org.tokend.template.features.wallet.details

import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange

class UnknownDetailsActivity : BalanceChangeDetailsActivity() {

    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.unknown_balance_change_details_title)

        displayEffect(item, cards_layout)
        displayBalanceChange(item, cards_layout)
        displayDate(item, cards_layout)
    }
}
