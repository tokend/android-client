package io.tokend.template.features.send

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.rxkotlin.subscribeBy
import io.tokend.template.R
import io.tokend.template.activities.BaseActivity
import io.tokend.template.features.send.logic.ConfirmPaymentRequestUseCase
import io.tokend.template.features.send.model.PaymentRequest
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
import java.math.BigDecimal

class PaymentConfirmationActivity : BaseActivity() {
    private lateinit var request: PaymentRequest
    private val adapter = DetailsItemsAdapter()
    private lateinit var mainDataView: BalanceChangeMainDataView

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_balance_change_confirmation)

        initToolbar()

        request =
            (intent.getSerializableExtra(PAYMENT_REQUEST_EXTRA) as? PaymentRequest)
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

    // region Display
    private fun displayDetails() {
        (top_info_text_view.layoutParams as? LinearLayout.LayoutParams)?.also {
            it.topMargin = 0
            top_info_text_view.layoutParams = it
        }

        mainDataView.displayOperationName(getString(R.string.balance_change_cause_payment))
        displayRecipient()
        displaySubjectIfNeeded()
        displayAmounts()
    }

    private fun displayRecipient() {
        adapter.addData(
            DetailsItem(
                text = request.recipient.displayedValue,
                hint = getString(R.string.tx_recipient),
                icon = ContextCompat.getDrawable(this, R.drawable.ic_account)
            )
        )
    }

    private fun displaySubjectIfNeeded() {
        val subject = request.paymentSubject
            ?: return

        adapter.addData(
            DetailsItem(
                text = subject,
                hint = getString(R.string.payment_description),
                icon = ContextCompat.getDrawable(this, R.drawable.ic_label_outline)
            )
        )
    }

    private fun displayAmounts() {
        displayToPay()
        displayToReceive()
    }

    private fun displayToPay() {
        val totalFee = request.fee.totalSenderFee
        val total = request.amount + totalFee

        mainDataView.displayAmount(total, request.asset, false)
        mainDataView.displayNonZeroFee(totalFee, request.asset)
    }

    private fun displayToReceive() {
        val totalFee = request.fee.recipientFee.total

        val totalActualFee =
            if (request.fee.senderPaysForRecipient)
                BigDecimal.ZERO
            else
                totalFee

        val total = (request.amount - totalActualFee).takeIf { it.signum() >= 0 } ?: BigDecimal.ZERO

        adapter.addOrUpdateItem(
            DetailsItem(
                id = TO_RECEIVE_AMOUNT_ITEM_ID,
                text = amountFormatter.formatAssetAmount(total, request.asset),
                hint = getString(R.string.to_receive),
                icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
            )
        )

        if (totalActualFee.signum() > 0) {
            adapter.addOrUpdateItem(
                DetailsItem(
                    id = RECIPIENT_FEE_ITEM_ID,
                    text = amountFormatter.formatAssetAmount(totalFee, request.asset),
                    hint = getString(R.string.tx_fee)
                )
            )
        }
    }
    // endregion

    private fun initConfirmButton() {
        confirm_button.apply {
            setOnClickListener { confirm() }

            // Prevent accidental click.
            isEnabled = false
            postDelayed({
                if (!isFinishing) {
                    isEnabled = true
                }
            }, 1500)
        }
    }

    private fun confirm() {
        val progress = ProgressDialogFactory.getDialog(this)

        ConfirmPaymentRequestUseCase(
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
                    toastManager.long(R.string.payment_successfully_sent)
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
        private const val PAYMENT_REQUEST_EXTRA = "payment_request"
        private val RECIPIENT_FEE_ITEM_ID = "recipient_fee".hashCode().toLong()
        private val TO_RECEIVE_AMOUNT_ITEM_ID = "to_receive_amount".hashCode().toLong()

        fun getBundle(paymentRequest: PaymentRequest) = Bundle().apply {
            putSerializable(PAYMENT_REQUEST_EXTRA, paymentRequest)
        }
    }
}
