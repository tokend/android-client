package org.tokend.template.features.send

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import android.support.v7.widget.SwitchCompat
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details_list.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.features.send.logic.ConfirmPaymentRequestUseCase
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.logic.transactions.TxManager
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.details.DetailsItem
import org.tokend.template.view.details.adapter.DetailsItemsAdapter
import org.tokend.template.view.details.ExtraViewProvider
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.ProgressDialogFactory
import org.tokend.wallet.xdr.FeeType
import java.math.BigDecimal

class PaymentConfirmationActivity : BaseActivity() {
    private lateinit var request: PaymentRequest
    private var switchEverChecked = false
    private val adapter = DetailsItemsAdapter()

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details_list)

        setSupportActionBar(toolbar)
        setTitle(R.string.payment_confirmation_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        request =
                (intent.getSerializableExtra(PAYMENT_REQUEST_EXTRA) as? PaymentRequest)
                        ?: return

        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter
        (details_list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        displayDetails()
        initConfirmButton()
        ElevationUtil.initScrollElevation(details_list, appbar_elevation_view)
    }

    // region Display
    private fun displayDetails() {
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
        adapter.addOrUpdateItem(
                DetailsItem(
                        id = TO_PAY_AMOUNT_ITEM_ID,
                        header = getString(R.string.to_pay),
                        text = amountFormatter.formatAssetAmount(request.amount, request.asset),
                        hint = getString(R.string.amount),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
                )
        )

        val totalFee =
                if (request.senderPaysRecipientFee)
                    request.senderFee.total + request.recipientFee.total
                else
                    request.senderFee.total

        val total = request.amount + totalFee

        if (totalFee.signum() > 0) {

            val feeExtraView =
                    ExtraViewProvider.getFeeView(this) {
                        Navigator.from(this).openFees(request.asset, FeeType.PAYMENT_FEE.value)
                    }

            adapter.addOrUpdateItem(
                    DetailsItem(
                            id = SENDER_FEE_ITEM_ID,
                            text = amountFormatter.formatAssetAmount(totalFee, request.asset),
                            hint = getString(R.string.tx_fee),
                            extraView = feeExtraView
                    )
            )

            adapter.addOrUpdateItem(
                    DetailsItem(
                            id = SENDER_TOTAL_ITEM_ID,
                            text = amountFormatter.formatAssetAmount(
                                    total,
                                    request.asset
                            ),
                            hint = getString(R.string.total_label)
                    )
            )
        }
    }

    private fun displayToReceive() {
        adapter.addOrUpdateItem(
                DetailsItem(
                        id = TO_RECEIVE_AMOUNT_ITEM_ID,
                        header = getString(R.string.to_receive),
                        text = amountFormatter.formatAssetAmount(request.amount, request.asset),
                        hint = getString(R.string.amount),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
                )
        )

        val totalFee = request.recipientFee.total

        val totalActualFee =
                if (request.senderPaysRecipientFee)
                    BigDecimal.ZERO
                else
                    totalFee

        val total = (request.amount - totalActualFee).takeIf { it.signum() >= 0 } ?: BigDecimal.ZERO

        if (totalFee.signum() > 0) {
            adapter.addOrUpdateItem(
                    DetailsItem(
                            id = RECIPIENT_FEE_ITEM_ID,
                            text = amountFormatter.formatAssetAmount(totalFee, request.asset),
                            hint = getString(R.string.tx_fee),
                            isEnabled = !request.senderPaysRecipientFee
                    )
            )

            adapter.addOrUpdateItem(
                    DetailsItem(
                            id = RECIPIENT_TOTAL_ITEM_ID,
                            text = amountFormatter.formatAssetAmount(
                                    total,
                                    request.asset
                            ),
                            hint = getString(R.string.total_label)
                    )
            )

            val payRecipientFeeSwitch = SwitchCompat(this)
                    .apply {
                        val setListener = {
                            setOnCheckedChangeListener { _, isChecked ->
                                switchEverChecked = true
                                request.senderPaysRecipientFee = isChecked
                                displayAmounts()
                            }
                        }

                        if (switchEverChecked) {
                            isChecked = !request.senderPaysRecipientFee

                            post {
                                isChecked = request.senderPaysRecipientFee
                                setListener()
                            }
                        } else {
                            setListener()
                        }
                    }

            adapter.addOrUpdateItem(
                    DetailsItem(
                            id = PAY_RECIPIENT_FEE_ITEM_ID,
                            text = getString(R.string.pay_recipient_fee_action),
                            extraView = payRecipientFeeSwitch
                    )
            )
        }
    }
    // endregion

    private fun initConfirmButton() {
        confirm_button.onClick { confirm() }
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
        private val SENDER_FEE_ITEM_ID = "sender_fee".hashCode().toLong()
        private val RECIPIENT_FEE_ITEM_ID = "recipient_fee".hashCode().toLong()
        private val SENDER_TOTAL_ITEM_ID = "sender_total".hashCode().toLong()
        private val RECIPIENT_TOTAL_ITEM_ID = "recipient_total".hashCode().toLong()
        private val TO_PAY_AMOUNT_ITEM_ID = "to_pay_amount".hashCode().toLong()
        private val TO_RECEIVE_AMOUNT_ITEM_ID = "to_receive_amount".hashCode().toLong()
        private val PAY_RECIPIENT_FEE_ITEM_ID = "pay_recipient_fee".hashCode().toLong()
    }
}
