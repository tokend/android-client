package org.tokend.template.base.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_send.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.layout_amount_with_spinner.*
import kotlinx.android.synthetic.main.layout_balance_card.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.models.Fee
import org.tokend.template.R
import org.tokend.template.base.activities.PaymentConfirmationActivity
import org.tokend.template.base.activities.WalletEventsListener
import org.tokend.template.base.logic.FeeManager
import org.tokend.template.base.logic.payment.PaymentRequest
import org.tokend.template.base.logic.repository.AccountDetailsRepository
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.template.base.view.AmountEditTextWrapper
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.base.view.util.SimpleTextWatcher
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.isTransferable
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.Permission
import org.tokend.template.util.QrScannerUtil
import org.tokend.wallet.Base32Check
import ua.com.radiokot.pc.util.text_validators.EmailValidator
import java.math.BigDecimal

class SendFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()
    private val cameraPermission = Permission(Manifest.permission.CAMERA, 404)
    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )
    private lateinit var amountEditTextWrapper: AmountEditTextWrapper

    private var isLoading = false
        set(value) {
            field = value
            loadingIndicator.setLoading(value)
            updateConfirmAvailability()
        }

    private var canConfirm: Boolean = false
        set(value) {
            field = value
            go_to_confirmation_button.enabled = value
        }

    private var asset: String = ""
        set(value) {
            field = value
            onAssetChanged()
        }
    private var assetBalance: BigDecimal = BigDecimal.ZERO

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)

        toolbar.title = getString(R.string.send_title)

        initFields()
        initButtons()

        arguments?.getString(ASSET_EXTRA)?.let { requiredAsset ->
            asset = requiredAsset
        }

        initAssetSelection()
        initSwipeRefresh()

        subscribeToBalances()
        balancesRepository.updateIfNotFresh()

        canConfirm = false
    }

    // region Init
    private fun initAssetSelection() {
        asset_spinner.onItemSelected {
            asset = it.text
        }
    }

    private fun initButtons() {
        go_to_confirmation_button.onClick {
            tryToConfirm()
        }

        scan_qr_button.onClick {
            tryOpenQrScanner()
        }
    }

    private fun initFields() {
        amountEditTextWrapper = AmountEditTextWrapper(amount_edit_text)
        amountEditTextWrapper.onAmountChanged { _, _ ->
            checkAmount()
            updateConfirmAvailability()
        }

        recipient_edit_text.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                recipient_edit_text.error = null
                updateConfirmAvailability()
            }
        })

        subject_edit_text.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                updateConfirmAvailability()
            }
        })
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { balancesRepository.update() }
    }
    // endregion

    // region Balance
    private var balancesDisposable: CompositeDisposable? = null

    private fun subscribeToBalances() {
        balancesDisposable?.dispose()
        balancesDisposable = CompositeDisposable(
                balancesRepository.itemsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            onBalancesUpdated()
                        },
                balancesRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { swipe_refresh.isRefreshing = it }
        ).also { it.addTo(compositeDisposable) }
    }

    private fun onBalancesUpdated() {
        updateBalance()
        displayBalance()
        displayTransferableAssets()
        checkAmount()
        updateConfirmAvailability()
    }

    private fun updateBalance() {
        assetBalance = balancesRepository.itemsSubject.value
                .find { it.asset == asset }
                ?.balance ?: BigDecimal.ZERO
    }

    private fun displayBalance() {
        balance_text_view.text = getString(R.string.template_balance,
                AmountFormatter.formatAssetAmount(assetBalance),
                asset
        )
    }

    private fun displayTransferableAssets() {
        val transferableAssets = balancesRepository.itemsSubject.value
                .mapNotNull {
                    it.assetDetails
                }
                .filter {
                    it.isTransferable()
                }
                .map {
                    it.code
                }

        if (transferableAssets.isEmpty()) {
            error_empty_view.showEmpty(R.string.error_no_transferable_assets)
            return
        }

        asset_spinner.setSimpleItems(transferableAssets, asset)
    }
    // endregion

    private fun tryOpenQrScanner() {
        cameraPermission.check(this) {
            QrScannerUtil.openScanner(this)
        }
    }

    // region Validation
    private fun checkAmount() {
        if (amountEditTextWrapper.scaledAmount > assetBalance) {
            amount_edit_text.error = getString(R.string.error_insufficient_balance)
        } else {
            amount_edit_text.error = null
        }
    }

    private fun checkRecipient() {
        val recipient = recipient_edit_text.text.toString().trim()

        val validAccountId = Base32Check.isValid(Base32Check.VersionByte.ACCOUNT_ID,
                recipient.toCharArray())
        val validEmail = EmailValidator.isValid(recipient)

        if (recipient.isEmpty()) {
            recipient_edit_text.error = getString(R.string.error_cannot_be_empty)
        } else if (!validAccountId && !validEmail) {
            recipient_edit_text.error = getString(R.string.error_invalid_recipient)
        } else {
            recipient_edit_text.error = null
        }
    }

    private fun updateConfirmAvailability() {
        canConfirm = !isLoading
                && !amount_edit_text.hasError()
                && !recipient_edit_text.hasError()
                && !subject_edit_text.hasError()
                && amountEditTextWrapper.scaledAmount.signum() > 0
    }
    // endregion

    private fun tryToConfirm() {
        checkRecipient()
        updateConfirmAvailability()
        if (canConfirm) {
            confirm()
        }
    }

    private class SendToYourselfException : Exception()

    private fun confirm() {
        val amount = amountEditTextWrapper.scaledAmount
        val asset = this.asset
        val subject = subject_edit_text.text.toString().trim().takeIf { it.isNotEmpty() }

        val recipient = recipient_edit_text.text.toString().trim()

        val data = object {
            lateinit var senderAccount: String
            lateinit var senderBalance: String
            lateinit var recipientAccount: String
        }

        Single.zip(
                getOurAccountId(),
                getRecipientAccountId(recipient),
                BiFunction { t1: String, t2: String -> Pair(t1, t2) }
        )
                .flatMap { (currentAccount, recipientAccount) ->
                    if (currentAccount == recipientAccount) {
                        throw SendToYourselfException()
                    }

                    data.senderAccount = currentAccount
                    data.recipientAccount = recipientAccount

                    Single.zip(
                            getOurBalanceId(asset),
                            getRecipientBalanceId(recipientAccount, asset),
                            BiFunction { t1: String, t2: String -> Pair(t1, t2) }
                    )
                }
                .flatMap { (currentBalance) ->
                    data.senderBalance = currentBalance

                    Single.zip(
                            FeeManager(apiProvider)
                                    .getPaymentFee(data.senderAccount, asset, amount, true),
                            FeeManager(apiProvider)
                                    .getPaymentFee(data.recipientAccount, asset, amount, false),
                            BiFunction { t1: Fee, t2: Fee -> Pair(t1, t2) }
                    )
                }
                .map { (senderFee, recipientFee) ->
                    PaymentRequest(
                            amount = amount,
                            asset = asset,
                            recipientFee = recipientFee,
                            senderFee = senderFee,
                            senderBalanceId = data.senderBalance,
                            recipientAccountId = data.recipientAccount,
                            recipientNickname = recipient,
                            paymentSubject = subject
                    )
                }
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnEvent { _, _ ->
                    isLoading = false
                }
                .subscribeBy(
                        onSuccess = { request ->
                            Navigator.openPaymentConfirmation(this,
                                    PAYMENT_CONFIRMATION_REQUEST, request)
                        },
                        onError = {
                            when (it) {
                                is AccountDetailsRepository.NoDetailsFoundException,
                                is NoRecipientBalanceException ->
                                    recipient_edit_text.setErrorAndFocus(
                                            R.string.error_invalid_recipient
                                    )
                                is SendToYourselfException ->
                                    recipient_edit_text.setErrorAndFocus(
                                            R.string.error_cannot_send_to_yourself
                                    )
                                else ->
                                    errorHandlerFactory.getDefault().handle(it)
                            }
                            updateConfirmAvailability()
                        }
                )
                .addTo(compositeDisposable)
    }

    // region Pre confirmation requests.
    private fun getOurAccountId(): Single<String> {
        return walletInfoProvider.getWalletInfo()?.accountId.toMaybe()
                .switchIfEmpty(Single.error(
                        IllegalStateException("Cannot obtain current account ID")
                ))
    }

    private fun getRecipientAccountId(recipient: String): Single<String> {
        return if (Base32Check.isValid(Base32Check.VersionByte.ACCOUNT_ID,
                        recipient.toCharArray()))
            Single.just(recipient)
        else
            repositoryProvider.accountDetails().getAccountIdByEmail(recipient)
    }

    private fun getOurBalanceId(asset: String): Single<String> {
        return balancesRepository
                .updateIfNotFreshDeferred()
                .andThen(
                        balancesRepository.itemsSubject.value
                                .find { it.asset == asset }
                                ?.balanceId
                                .toMaybe()
                                .switchIfEmpty(Single.error(
                                        IllegalStateException("No balance ID found for $asset")
                                ))
                )
    }

    private class NoRecipientBalanceException : Exception()

    private fun getRecipientBalanceId(recipientAccountId: String, asset: String): Single<String> {
        return repositoryProvider.accountDetails().getBalancesByAccountId(recipientAccountId)
                .flatMap { balances ->
                    balances.find { it.asset == asset }
                            .toMaybe()
                            .switchIfEmpty(Single.error(
                                    NoRecipientBalanceException()
                            ))
                }
                .map { balance ->
                    balance.balanceId
                }
    }
    // endreigon.

    private fun onAssetChanged() {
        updateBalance()
        checkAmount()
        updateConfirmAvailability()
        displayBalance()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermission.handlePermissionResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        QrScannerUtil.getStringFromResult(requestCode, resultCode, data)?.also {
            recipient_edit_text.setText(it)
            recipient_edit_text.setSelection(recipient_edit_text.text.length)
            checkRecipient()
            updateConfirmAvailability()
        }

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PAYMENT_CONFIRMATION_REQUEST -> {
                    val confirmedRequest =
                            data?.getSerializableExtra(
                                    PaymentConfirmationActivity.PAYMENT_REQUEST_EXTRA
                            ) as? PaymentRequest
                    if (confirmedRequest != null) {
                        (activity as? WalletEventsListener)
                                ?.onPaymentRequestConfirmed(confirmedRequest)
                    }
                }
            }
        }
    }

    companion object {
        private const val ASSET_EXTRA = "asset"
        const val ID = 1118L
        val PAYMENT_CONFIRMATION_REQUEST = "confirm_payment".hashCode() and 0xffff

        fun newInstance(asset: String? = null): SendFragment {
            val fragment = SendFragment()
            fragment.arguments = Bundle().apply {
                putString(ASSET_EXTRA, asset)
            }
            return fragment
        }
    }
}