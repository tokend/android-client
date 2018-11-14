package org.tokend.template.base.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.base.logic.payment.ConfirmPaymentRequestUseCase
import org.tokend.template.base.logic.payment.PaymentRequest
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ProgressDialogFactory
import org.tokend.template.util.ToastManager

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
    private fun displayDetails() {
        cards_layout.removeAllViews()

        displayRecipient()
        displaySubjectIfNeeded()
        displayAmount()
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

    private fun displayAmount() {
        InfoCard(cards_layout)
                .setHeading(R.string.amount,
                        "${AmountFormatter.formatAssetAmount(request.amount)} ${request.asset}")
                .addRow(R.string.tx_fee,
                        "${AmountFormatter.formatAssetAmount(request.senderFee.total)
                        } ${request.senderFee.asset}")
                .addRow(R.string.tx_recipient_fee,
                        "${AmountFormatter.formatAssetAmount(request.recipientFee.total)
                        } ${request.recipientFee.asset}")
                .addSwitcherRow(R.string.pay_recipient_fee_action,
                        CompoundButton.OnCheckedChangeListener { _, isChecked ->
                            request.senderPaysRecipientFee = isChecked
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
                            ToastManager(this).long(R.string.payment_successfully_sent)
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
