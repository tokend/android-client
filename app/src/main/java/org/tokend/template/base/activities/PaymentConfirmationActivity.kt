package org.tokend.template.base.activities

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_payment_confirmation.*
import org.jetbrains.anko.onCheckedChange
import org.tokend.template.R
import org.tokend.template.base.logic.payment.PaymentManager
import org.tokend.template.base.logic.payment.PaymentRequest
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory
import java.math.BigDecimal

class PaymentConfirmationActivity : BaseActivity() {
    private lateinit var request: PaymentRequest

    private var payRecipientFee = false
        set(value) {
            field = value
            displayDetails()
        }

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_payment_confirmation)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        request =
                (intent.getSerializableExtra(PAYMENT_REQUEST_EXTRA) as? PaymentRequest)
                ?: return

        initPayRecipientFeeSwitch()
        displayDetails()
    }

    private fun initPayRecipientFeeSwitch() {
        pay_recipient_fee_switch.onCheckedChange { _, checked ->
            payRecipientFee = checked
        }
    }

    // region Display
    private fun displayDetails() {
        cards_layout.removeAllViews()

        displayRecipient()
        displaySubjectIfNeeded()
        displayToPay()
        displayToReceive()
    }

    private fun displayRecipient() {
        InfoCard(cards_layout)
                .setHeading(R.string.tx_recipient, null)
                .addRow(request.recipientNickname, null)
    }

    private fun displaySubjectIfNeeded() {
        request.paymentSubject?.let { subject ->
            InfoCard(cards_layout)
                    .setHeading(R.string.payment_description, null)
                    .addRow(subject, null)
        }
    }

    private fun displayToPay() {
        val fixedFeeTotal = request.senderFee.fixed +
                if (payRecipientFee) request.recipientFee.fixed else BigDecimal.ZERO

        val percentFeeTotal = request.senderFee.percent +
                if (payRecipientFee) request.recipientFee.percent else BigDecimal.ZERO

        val toPay = request.amount + fixedFeeTotal + percentFeeTotal

        InfoCard(cards_layout)
                .setHeading(R.string.to_pay,
                        "${AmountFormatter.formatAssetAmount(toPay)} ${request.asset}")
                .addRow(R.string.amount,
                        "+${AmountFormatter.formatAssetAmount(request.amount,
                                minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                        } ${request.asset}")
                .addRow(R.string.tx_fixed_fee,
                        "+${AmountFormatter.formatAssetAmount(fixedFeeTotal,
                                minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                        } ${request.asset}")
                .addRow(R.string.tx_percent_fee,
                        "+${AmountFormatter.formatAssetAmount(percentFeeTotal,
                                minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                        } ${request.asset}")
    }

    private fun displayToReceive() {
        val toReceive = (request.amount -
                if (!payRecipientFee) request.recipientFee.total else BigDecimal.ZERO)
                .takeIf { it.signum() > 0 } ?: BigDecimal.ZERO
        receive_total_text_view.text =
                "${AmountFormatter.formatAssetAmount(toReceive)} ${request.asset}"

        receive_amount_text_view.text = "+${AmountFormatter.formatAssetAmount(request.amount,
                minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)} ${request.asset}"

        if (payRecipientFee) {
            receive_fee_layout.visibility = View.GONE
        } else {
            receive_fee_layout.visibility = View.VISIBLE

            receive_fixed_fee_text_view.text =
                    "-${AmountFormatter.formatAssetAmount(request.recipientFee.fixed,
                            minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)} ${request.asset}"
            receive_percent_fee_text_view.text =
                    "-${AmountFormatter.formatAssetAmount(request.recipientFee.percent,
                            minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)} ${request.asset}"
        }
    }
    // endregion

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.confirmation, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.confirm -> confirm()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirm() {
        val progress = ProgressDialog(this)
        progress.isIndeterminate = true
        progress.setMessage(getString(R.string.processing_progress))
        progress.setCancelable(false)

        request.senderPaysRecipientFee = payRecipientFee

        PaymentManager(repositoryProvider, walletInfoProvider, accountProvider,
                TxManager(apiProvider))
                .submit(request)
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .bindUntilEvent(lifecycle(), ActivityEvent.DESTROY)
                .doOnSubscribe {
                    progress.show()
                }
                .doOnEvent { _, _ ->
                    progress.dismiss()
                }
                .subscribeBy(
                        onSuccess = {
                            progress.dismiss()
                            ToastManager.long(R.string.payment_successfully_sent)
                            finishWithSuccess()
                        },
                        onError = {
                            ErrorHandlerFactory.getDefault().handle(it)
                        }
                )
    }

    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        const val PAYMENT_REQUEST_EXTRA = "payment_request"
    }
}
