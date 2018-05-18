package org.tokend.template.features.withdraw

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.zxing.integration.android.IntentIntegrator
import com.trello.rxlifecycle2.android.FragmentEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_withdraw.*
import kotlinx.android.synthetic.main.layout_amount_with_spinner.*
import kotlinx.android.synthetic.main.layout_balance_card.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.base.fragments.BaseFragment
import org.tokend.template.base.fragments.ToolbarProvider
import org.tokend.template.base.logic.FeeManager
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.template.base.view.AmountEditTextWrapper
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.base.view.util.SimpleTextWatcher
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.isWithdrawable
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.Permission
import org.tokend.template.util.error_handlers.ErrorHandlerFactory
import java.math.BigDecimal

class WithdrawFragment : BaseFragment(), ToolbarProvider {
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
        return inflater.inflate(R.layout.fragment_withdraw, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)

        toolbar.title = getString(R.string.withdraw_title)

        initFields()
        initButtons()
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

        address_edit_text.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                checkAddress()
                updateConfirmAvailability()
            }
        })
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { balancesRepository.update() }
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

    // region Balances
    private var balancesDisposable: CompositeDisposable? = null

    private fun subscribeToBalances() {
        balancesDisposable?.dispose()
        balancesDisposable = CompositeDisposable(
                balancesRepository.itemsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                        .subscribe {
                            onBalancesUpdated()
                        },
                balancesRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                        .subscribe { swipe_refresh.isRefreshing = it }
        )
    }

    private fun onBalancesUpdated() {
        updateBalance()
        displayBalance()
        displayWithdrawableAssets()
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

    private fun displayWithdrawableAssets() {
        val withdrawableAssets = balancesRepository.itemsSubject.value
                .mapNotNull {
                    it.assetDetails
                }
                .filter {
                    it.isWithdrawable()
                }
                .map {
                    it.code
                }

        asset_spinner.setSimpleItems(withdrawableAssets)
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

    private fun checkAddress() {
        if (address_edit_text.text.isBlank()) {
            address_edit_text.error = getString(R.string.error_cannot_be_empty)
        } else {
            address_edit_text.error = null
        }
    }

    private fun updateConfirmAvailability() {
        canConfirm = !isLoading
                && !amount_edit_text.hasError()
                && !address_edit_text.hasError()
                && amountEditTextWrapper.scaledAmount.signum() > 0
    }
    // endregion

    private fun tryToConfirm() {
        checkAddress()
        updateConfirmAvailability()
        if (canConfirm) {
            confirm()
        }
    }

    private fun confirm() {
        val amount = amountEditTextWrapper.scaledAmount
        val asset = this.asset
        val address = address_edit_text.text.toString().trim()

        walletInfoProvider.getWalletInfo()?.accountId.toMaybe()
                .switchIfEmpty(Single.error(IllegalStateException("Cannot obtain current account ID")))
                .flatMap { accountId ->
                    FeeManager(apiProvider).getWithdrawalFee(accountId, asset, amount)
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
                        onSuccess = { fee ->
                            val request = WithdrawalRequest(
                                    asset = asset,
                                    amount = amount,
                                    destinationAddress = address,
                                    fee = fee
                            )

                            Navigator.openWithdrawalConfirmation(activity!!,
                                    4511, request)
                        },
                        onError = { ErrorHandlerFactory.getDefault().handle(it) }
                )
    }

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
            address_edit_text.setText(scanResult.contents)
        }
    }
}