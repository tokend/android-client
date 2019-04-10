package org.tokend.template.features.wallet.details

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.features.wallet.view.BalanceChangeIconFactory
import org.tokend.template.view.details.DetailsItem
import org.tokend.template.view.details.adapter.DetailsItemsAdapter
import org.tokend.template.view.util.LocalizedName
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
                                   adapter: DetailsItemsAdapter) {
        adapter.addData(
                DetailsItem(
                        text = DateFormatter(this).formatLong(item.date),
                        hint = getString(R.string.date),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_date)
                )
        )
    }

    protected open fun displayEffect(item: BalanceChange,
                                     adapter: DetailsItemsAdapter) {
        val iconFactory = BalanceChangeIconFactory(this)

        adapter.addData(
                DetailsItem(
                        text = LocalizedName(this).forBalanceChangeAction(item.action),
                        hint = getString(R.string.tx_effect),
                        icon = iconFactory.get(item)
                )
        )
    }

    protected open fun displayBalanceChange(item: BalanceChange,
                                            adapter: DetailsItemsAdapter) {
        val asset = item.assetCode

        adapter.addData(
                DetailsItem(
                        text = amountFormatter.formatAssetAmount(item.amount, asset),
                        hint = getString(R.string.amount),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
                )
        )
        if (item.fee.total.signum() > 0) {
            adapter.addData(
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(item.fee.total, asset),
                            hint = getString(R.string.tx_fee)
                    )
            )
        }
    }

    companion object {
        const val BALANCE_CHANGE_EXTRA = "balance_change"
        private const val LOG_TAG = "BlncChDetailsActivity"
    }
}