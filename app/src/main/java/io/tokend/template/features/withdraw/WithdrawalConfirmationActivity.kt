package io.tokend.template.features.withdraw

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.rxkotlin.subscribeBy
import io.tokend.template.R
import io.tokend.template.activities.BaseActivity
import io.tokend.template.features.withdraw.logic.ConfirmWithdrawalRequestUseCase
import io.tokend.template.features.withdraw.model.WithdrawalRequest
import io.tokend.template.logic.TxManager
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.view.balancechange.BalanceChangeMainDataView
import io.tokend.template.view.details.DetailsItem
import io.tokend.template.view.details.adapter.DetailsItemsAdapter
import io.tokend.template.view.util.ElevationUtil
import io.tokend.template.view.util.ProgressDialogFactory
import kotlinx.android.synthetic.main.activity_balance_change_confirmation.*
import kotlinx.android.synthetic.main.appbar_with_balance_change_main_data.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.layout_balance_change_main_data.*
import kotlinx.android.synthetic.main.toolbar.*

class WithdrawalConfirmationActivity : BaseActivity() {
    private lateinit var request: WithdrawalRequest
    private val adapter = DetailsItemsAdapter()
    private lateinit var mainDataView: BalanceChangeMainDataView

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_balance_change_confirmation)

        initToolbar()

        request =
            (intent.getSerializableExtra(WITHDRAWAL_REQUEST_EXTRA) as? WithdrawalRequest)
                ?: return

        mainDataView = BalanceChangeMainDataView(appbar, amountFormatter)

        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter

        displayDetails()
        initConfirmButton()
        ElevationUtil.initScrollElevation(details_list, appbar_elevation_view)
    }

    private fun initToolbar() {
        toolbar.background = ColorDrawable(ContextCompat.getColor(this, R.color.background))
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun displayDetails() {
        (top_info_text_view.layoutParams as? LinearLayout.LayoutParams)?.also {
            it.topMargin = 0
            top_info_text_view.layoutParams = it
        }

        mainDataView.displayOperationName(getString(R.string.balance_change_cause_withdrawal_request))
        displayAmount()
        displayDestinationAddress()
        displayExternalFeesWarning()
    }

    private fun displayAmount() {
        val asset = request.asset
        val total = request.amount + request.fee.total

        mainDataView.displayAmount(total, asset, false)
        mainDataView.displayNonZeroFee(request.fee.total, asset)
    }

    private fun displayDestinationAddress() {
        adapter.addData(
            DetailsItem(
                text = request.destinationAddress,
                hint = getString(R.string.tx_withdrawal_destination),
                icon = ContextCompat.getDrawable(this, R.drawable.ic_forward)
            )
        )
    }

    private fun displayExternalFeesWarning() {
        adapter.addData(
            DetailsItem(
                text = getString(R.string.withdrawal_fee_warning),
                isEnabled = false
            )
        )
    }

    private fun initConfirmButton() {
        confirm_button.setOnClickListener { confirm() }
    }

    private fun confirm() {
        val progress = ProgressDialogFactory.getDialog(this)

        ConfirmWithdrawalRequestUseCase(
            request,
            accountProvider,
            repositoryProvider,
            TxManager(apiProvider)
        )
            .perform()
            .compose(ObservableTransformers.defaultSchedulersCompletable())
            .doOnSubscribe {
                progress.show()
            }
            .doOnTerminate {
                progress.dismiss()
            }
            .subscribeBy(
                onComplete = {
                    progress.dismiss()
                    toastManager.long(R.string.withdrawal_request_created)
                    finishWithSuccess()
                },
                onError = {
                    errorHandlerFactory.getDefault().handle(it)
                }
            )
    }

    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        const val WITHDRAWAL_REQUEST_EXTRA = "withdrawal_request"

        fun getBundle(withdrawalRequest: WithdrawalRequest) = Bundle().apply {
            putSerializable(WITHDRAWAL_REQUEST_EXTRA, withdrawalRequest)
        }
    }
}
