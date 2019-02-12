package org.tokend.template.features.wallet.details

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.view.InfoCard
import org.tokend.template.view.util.formatter.DateFormatter

abstract class BalanceChangeDetailsActivity : BaseActivity() {
    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val intentItem = intent.getSerializableExtra(BALANCE_CHANGE_EXTRA)
                as? BalanceChange

        if (intentItem != null) {
            displayDetails(intentItem)
        } else {
            Log.e(LOG_TAG, "Unable to get serializable $BALANCE_CHANGE_EXTRA")
            finish()
            return
        }
    }

    protected abstract fun displayDetails(item: BalanceChange)

    protected open fun displayDate(item: BalanceChange,
                                   cardsLayout: ViewGroup) {
        InfoCard(cardsLayout)
                .setHeading(R.string.date, null)
                .addRow(DateFormatter(this).formatLong(item.date), null)
    }

    companion object {
        const val BALANCE_CHANGE_EXTRA = "balance_change"
        private const val LOG_TAG = "BlncChDetailsActivity"
    }
}