package org.tokend.template.features.history.details

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.android.synthetic.main.activity_balance_change_details.*
import kotlinx.android.synthetic.main.appbar_with_balance_change_main_data.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.features.history.model.BalanceChange
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
            finishWithMissingArgError(BALANCE_CHANGE_EXTRA)
            return
        }
    }

    protected open fun initToolbar() {
        toolbar.background = ColorDrawable(ContextCompat.getColor(this, R.color.background))
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
        mainDataView.displayAmount(item.totalAmount, item.asset, item.isReceived)
    }

    protected open fun displayFee(item: BalanceChange) {
        mainDataView.displayNonZeroFee(item.fee.total, item.asset)
    }

    protected open fun displayDate(item: BalanceChange) {
        mainDataView.displayDate(item.date)
    }

    companion object {
        private const val BALANCE_CHANGE_EXTRA = "balance_change"

        fun getBundle(balanceChange: BalanceChange) = Bundle().apply {
            putSerializable(BALANCE_CHANGE_EXTRA, balanceChange)
        }
    }
}