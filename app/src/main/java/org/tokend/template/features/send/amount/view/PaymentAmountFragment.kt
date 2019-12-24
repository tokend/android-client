package org.tokend.template.features.send.amount.view

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_amount_input.*
import kotlinx.android.synthetic.main.layout_payment_description.*
import kotlinx.android.synthetic.main.layout_payment_description.view.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.extensions.withArguments
import org.tokend.template.features.amountscreen.view.AmountInputFragment
import org.tokend.template.features.send.amount.logic.PaymentFeeLoader
import org.tokend.template.features.send.amount.model.PaymentAmountData
import org.tokend.template.features.send.model.PaymentFee
import org.tokend.template.logic.FeeManager
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.balancepicker.BalancePickerBottomDialog
import org.tokend.wallet.Base32Check
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class PaymentAmountFragment : AmountInputFragment() {
    private val recipient: String?
        get() = arguments?.getString(RECIPIENT_EXTRA)

    private val recipientAccount: String?
        get() = arguments?.getString(RECIPIENT_ACCOUNT_EXTRA)

    private lateinit var feeView: PaymentFeeView
    private lateinit var feeManager: FeeManager
    private var fee: PaymentFee? = null
    private var isFeeLoaded = false
    private val feeIsLoading: Boolean
        get() = feeView.isLoading

    private var canContinue: Boolean = false
        set(value) {
            field = value
            action_button.isEnabled = value
        }

    override fun onInitAllowed() {
        initFeeView()
        updateFeeView()
        super.onInitAllowed()
    }

    private fun initFeeView() {
        feeManager = FeeManager(apiProvider)
        feeView = PaymentFeeView(requireContext())
        feeView.onPayRecipientFeeChanged { choice ->
            fee?.senderPaysForRecipient = choice
            updateActionButtonAvailability()
        }
    }

    override fun initFields() {
        super.initFields()

        amountWrapper.onAmountChanged { scaledAmount, _ ->
            if (scaledAmount.signum() > 0) {
                loadFees(true)
            } else {
                loadFees(false)
            }

            updateActionButtonAvailability()
        }
    }

    override fun getTitleText(): String? {
        val recipient = recipient ?: return null

        val displayRecipient =
                if (Base32Check.isValid(Base32Check.VersionByte.ACCOUNT_ID,
                                recipient.toCharArray()))
                    recipient.substring(0..3) + "..." +
                            recipient.substring(recipient.length - 5, recipient.length - 1)
                else
                    recipient

        return getString(R.string.template_tx_to, displayRecipient)
    }

    override fun getActionButtonText(): String {
        return getString(R.string.go_to_confirmation_btn_label)
    }

    override fun getBalancePicker(): BalancePickerBottomDialog {
        return BalancePickerBottomDialog(
                requireContext(),
                amountFormatter,
                balanceComparator,
                balancesRepository
        ) { balance ->
            balance.asset.isTransferable
        }
    }

    override fun getExtraView(parent: ViewGroup): View? {
        val view = requireContext().layoutInflater
                .inflate(R.layout.layout_payment_description, parent, false)

        view.payment_description_edit_text.onEditorAction {
            postResult()
        }

        return view
    }

    override fun getExtraAmountView(parent: ViewGroup): View? {
        return feeView
    }

    override fun postResult() {
        if (!canContinue) {
            return
        }

        val amount = amountWrapper.scaledAmount
        val description = payment_description_edit_text.text
                .toString()
                .trim()
                .takeIf { it.isNotEmpty() }
        val fee = this.fee
                ?: return
        val asset = balance?.asset
                ?: return

        resultSubject.onNext(
                PaymentAmountData(
                        amount = amount,
                        asset = asset,
                        description = description,
                        fee = fee
                )
        )
    }

    // region Fees
    private var feesLoadingDisposable: Disposable? = null

    private fun loadFees(scheduleDelayed: Boolean) {
        feesLoadingDisposable?.dispose()

        val amount = amountWrapper.scaledAmount
        val assetCode = asset.code
        val recipientAccount = recipientAccount ?: return

        val delaySingle =
                if (scheduleDelayed)
                    Single.just(true).delay(FEES_LOADING_DELAY, TimeUnit.MILLISECONDS)
                else
                    Single.just(true)

        feesLoadingDisposable = delaySingle
                .flatMap {
                    PaymentFeeLoader(
                            walletInfoProvider,
                            feeManager
                    )
                            .load(
                                    amount,
                                    assetCode,
                                    recipientAccount
                            )
                }
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .retry(3)
                .doOnSubscribe {
                    isFeeLoaded = false
                    feeView.isLoading = true
                    updateActionButtonAvailability()
                }
                .doOnEvent { _, _ ->
                    feeView.isLoading = false
                }
                .subscribeBy(
                        onSuccess = this::onNewFee,
                        onError = {
                            isFeeLoaded = false
                            updateActionButtonAvailability()
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
                .addTo(compositeDisposable)
    }

    private fun onNewFee(fee: PaymentFee) {
        isFeeLoaded = true
        this.fee = fee
        fee.senderPaysForRecipient = feeView.isPayingRecipientFee
        updateFeeView()
        updateActionButtonAvailability()
    }

    private fun updateFeeView() {
        val asset = balance?.asset
                ?: return
        fee?.apply {
            feeView.setFees(senderFee.total, recipientFee.total, asset, amountFormatter)
        }
    }
    // endregion

    override fun isEnoughBalance(): Boolean {
        val toPayAmount = amountWrapper.scaledAmount +
                (fee?.totalSenderFee?.takeIf { !feeIsLoading } ?: BigDecimal.ZERO)
        return toPayAmount <= (balance?.available ?: BigDecimal.ZERO)
    }

    override fun updateActionButtonAvailability() {
        checkAmount()

        canContinue = !hasError
                && amountWrapper.scaledAmount.signum() > 0
                && isFeeLoaded
                && !feeIsLoading
    }

    override fun getSmallSizingHeightThreshold(): Int {
        return requireContext().dip(248)
    }

    override fun getMinLayoutHeight(): Int {
        return requireContext().dip(248)
    }

    companion object {
        private const val RECIPIENT_EXTRA = "recipient"
        private const val RECIPIENT_ACCOUNT_EXTRA = "recipient_account"
        private const val FEES_LOADING_DELAY = 400L

        fun getBundle(recipient: String,
                      recipientAccount: String,
                      requiredAsset: String? = null) = Bundle().apply {
            putString(ASSET_EXTRA, requiredAsset)
            putString(RECIPIENT_EXTRA, recipient)
            putString(RECIPIENT_ACCOUNT_EXTRA, recipientAccount)
        }

        fun newInstance(bundle: Bundle): PaymentAmountFragment =
                PaymentAmountFragment().withArguments(bundle)
    }
}