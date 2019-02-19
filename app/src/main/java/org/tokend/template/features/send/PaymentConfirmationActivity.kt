package org.tokend.template.features.send

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.features.send.logic.ConfirmPaymentRequestUseCase
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.logic.transactions.TxManager
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.InfoCard
import org.tokend.template.view.util.ProgressDialogFactory
import java.math.BigDecimal

class PaymentConfirmationActivity : BaseActivity() {
    private lateinit var request: PaymentRequest

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        request =
                (intent.getSerializableExtra(PAYMENT_REQUEST_EXTRA) as? PaymentRequest)
                        ?: return

        displayDetails()
    }

    // region Display
    private fun displayDetails(payRecipientFee: Boolean = false) {
        cards_layout.removeAllViews()

        displayRecipient()
        displaySubjectIfNeeded()
        displayAmount(payRecipientFee)
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

    private fun displayAmount(payRecipientFee: Boolean) {
        val minDecimals = amountFormatter.getDecimalDigitsCount(request.asset)

        val senderFeeTotal: BigDecimal
        val senderFeeFixed: BigDecimal
        val senderFeePercent: BigDecimal

        val recipientFeeTotal: BigDecimal
        val recipientFeeFixed: BigDecimal
        val recipientFeePercent: BigDecimal

        if (payRecipientFee) {
            recipientFeeTotal = BigDecimal.ZERO
            recipientFeeFixed = BigDecimal.ZERO
            recipientFeePercent = BigDecimal.ZERO
            senderFeeTotal = request.senderFee.total + request.recipientFee.total
            senderFeeFixed = request.senderFee.fixed + request.recipientFee.fixed
            senderFeePercent = request.senderFee.percent + request.recipientFee.percent
        } else {
            recipientFeeTotal = request.recipientFee.total
            recipientFeeFixed = request.recipientFee.fixed
            recipientFeePercent = request.recipientFee.percent
            senderFeeTotal = request.senderFee.total
            senderFeeFixed = request.senderFee.fixed
            senderFeePercent = request.senderFee.percent
        }

        val toPay = request.amount + senderFeeTotal
        InfoCard(cards_layout)
                .setHeading(R.string.to_pay,
                        amountFormatter.formatAssetAmount(toPay, request.asset, minDecimals))
                .addRow(R.string.amount,
                        "+${amountFormatter.formatAssetAmount(request.amount,
                                request.asset, minDecimals)}")
                .addRow(R.string.fixed_fee,
                        "+${amountFormatter.formatAssetAmount(senderFeeFixed,
                                request.asset, minDecimals)}")
                .addRow(R.string.percent_fee,
                        "+${amountFormatter.formatAssetAmount(senderFeePercent,
                                request.asset, minDecimals)}")



        val toReceive = request.amount - recipientFeeTotal
        InfoCard(cards_layout)
                .setHeading(R.string.to_receive,
                        amountFormatter.formatAssetAmount(toReceive,
                                request.asset, minDecimals))
                .addRow(R.string.amount,
                        "+${amountFormatter.formatAssetAmount(request.amount,
                                request.asset, minDecimals)}")
                .addRow(R.string.fixed_fee,
                        "-${amountFormatter.formatAssetAmount(recipientFeeFixed,
                                request.asset, minDecimals)}")
                .addRow(R.string.percent_fee,
                        "-${amountFormatter.formatAssetAmount(recipientFeePercent,
                                request.asset, minDecimals)}")
                .addSwitcherRow(R.string.pay_recipient_fee_action, payRecipientFee,
                        CompoundButton.OnCheckedChangeListener { _, isChecked ->
                            request.senderPaysRecipientFee = isChecked
                            displayDetails(isChecked)
                        })

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
        val progress = ProgressDialogFactory.getTunedDialog(this)

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
        setResult(Activity.RESULT_OK,
                Intent().putExtra(PAYMENT_REQUEST_EXTRA, request))
        finish()
    }

    companion object {
        const val PAYMENT_REQUEST_EXTRA = "payment_request"
    }
}
