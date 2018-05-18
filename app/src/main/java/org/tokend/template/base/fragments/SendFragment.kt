package org.tokend.template.base.fragments

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.zxing.integration.android.IntentIntegrator
import com.trello.rxlifecycle2.android.FragmentEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
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
import org.tokend.template.util.error_handlers.ErrorHandlerFactory
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
        initAssets()

        subscribeToBalances()

        canConfirm = false
    }

    // region Init
    private fun initAssets() {
        val transferableAssets = balancesRepository.itemsSubject.value
                .map {
                    it.assetDetails
                }
                .filterNotNull()
                .filter {
                    it.isTransferable()
                }

        if (transferableAssets.isEmpty()) {
            error_empty_view.showEmpty(R.string.error_no_transferable_assets)
            return
        }

        asset_spinner.adapter = ArrayAdapter<String>(context, R.layout.spinner_item,
                transferableAssets.map { it.code })


        asset_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?,
                                        position: Int, id: Long) {
                asset = transferableAssets[position].code
            }
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
    // endregion

    // region Balance
    private var balancesDisposable: Disposable? = null

    private fun subscribeToBalances() {
        balancesDisposable?.dispose()
        balancesDisposable = balancesRepository.itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                .subscribe {
                    updateBalance()
                    displayBalance()
                }
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
    // endregion

    // region QR
    private fun tryOpenQrScanner() {
        cameraPermission.check(this) {
            openQrScanner()
        }
    }

    private fun openQrScanner() {
        IntentIntegrator.forSupportFragment(this)
                .setBeepEnabled(false)
                .setOrientationLocked(false)
                .setPrompt("")
                .initiateScan()
    }
    // endregion

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

    private fun confirm() {
        val amount = amountEditTextWrapper.scaledAmount
        val asset = this.asset
        val subject = subject_edit_text.text.toString().trim().takeIf { it.isNotEmpty() }

        val recipient = recipient_edit_text.text.toString().trim()

        val data = object {
            lateinit var senderAccount: String
            lateinit var senderBalance: String
            lateinit var recipientAccount: String
            lateinit var recipientBalance: String
        }

        Single.zip(
                getOurAccountId(),
                getRecipientAccountId(recipient),
                BiFunction { t1: String, t2: String -> Pair(t1, t2) }
        )
                .flatMap { (currentAccount, recipientAccount) ->
                    data.senderAccount = currentAccount
                    data.recipientAccount = recipientAccount

                    Single.zip(
                            getOurBalanceId(asset),
                            getRecipientBalanceId(recipientAccount, asset),
                            BiFunction { t1: String, t2: String -> Pair(t1, t2) }
                    )
                }
                .flatMap { (currentBalance, recipientBalance) ->
                    data.senderBalance = currentBalance
                    data.recipientBalance = recipientBalance

                    Single.zip(
                            FeeManager(apiProvider)
                                    .getPaymentFee(data.senderAccount, asset, amount),
                            FeeManager(apiProvider)
                                    .getPaymentFee(data.recipientBalance, asset, amount),
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
                            recipientBalanceId = data.recipientBalance,
                            recipientNickname = recipient,
                            paymentSubject = subject
                    )
                }
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnEvent { _, _ ->
                    isLoading = false
                }
                .subscribeBy(
                        onSuccess = { request ->
                            Navigator.openPaymentConfirmation(activity!!,
                                    4612, request)
                        },
                        onError = {
                            when (it) {
                                is AccountDetailsRepository.NoDetailsFoundException,
                                is NoRecipientBalanceException ->
                                    recipient_edit_text.setErrorAndFocus(
                                            R.string.error_invalid_recipient
                                    )
                                else ->
                                    ErrorHandlerFactory.getDefault().handle(it)
                            }
                            updateConfirmAvailability()
                        }
                )
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

        val scanResult = IntentIntegrator
                .parseActivityResult(requestCode, resultCode, data)

        if (scanResult != null && scanResult.contents != null) {
            recipient_edit_text.setText(scanResult.contents)
            recipient_edit_text.setSelection(recipient_edit_text.text.length)
            checkRecipient()
            updateConfirmAvailability()
        }
    }
}