package org.tokend.template.features.wallet.details

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import android.util.Log
import kotlinx.android.synthetic.main.activity_balance_change_details.*
import kotlinx.android.synthetic.main.appbar_with_balance_change_main_data.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.view.balancechange.BalanceChangeMainDataView
import org.tokend.template.view.details.adapter.DetailsItemsAdapter
import org.tokend.template.view.util.LocalizedName

abstract class BalanceChangeDetailsActivity : BaseActivity() {
    protected val adapter = DetailsItemsAdapter()
    protected lateinit var mainDataView: BalanceChangeMainDataView

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_balance_change_details)

        initToolbar()
        initMainDataView()
        initList()

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

    protected open fun initToolbar() {
        toolbar.background = ColorDrawable(Color.WHITE)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { finish() }
    }

    protected open fun initMainDataView() {
        mainDataView = BalanceChangeMainDataView(appbar, amountFormatter)
    }

    protected open fun initList() {
        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter
        (details_list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    }

    protected open fun displayDetails(item: BalanceChange) {
        displayOperationName(item)
        displayAmountAndFee(item)
        displayDate(item)
    }

    protected open fun displayOperationName(item: BalanceChange) {
        displayOperationName(LocalizedName(this).forBalanceChangeCause(item.cause))
    }

    protected open fun displayOperationName(operationName: String) {
        mainDataView.displayOperationName(operationName)
    }

    protected open fun displayAmountAndFee(item: BalanceChange) {
        displayAmount(item)
        displayFee(item)
    }

    protected open fun displayAmount(item: BalanceChange) {
        mainDataView.displayAmount(item.amount, item.assetCode, item.isReceived)
    }

    protected open fun displayFee(item: BalanceChange) {
        mainDataView.displayNonZeroFee(item.fee.total, item.assetCode)
    }

    protected open fun displayDate(item: BalanceChange) {
        mainDataView.displayDate(item.date)
    }

    companion object {
        const val BALANCE_CHANGE_EXTRA = "balance_change"
        private const val LOG_TAG = "BlncChDetailsActivity"
    }
}