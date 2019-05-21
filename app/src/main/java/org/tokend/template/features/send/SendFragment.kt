package org.tokend.template.features.send

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.appbar.*
import kotlinx.android.synthetic.main.fragment_user_flow.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.data.model.AssetRecord
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.features.send.amount.model.PaymentAmountAndDescription
import org.tokend.template.features.send.amount.view.PaymentAmountAndDescriptionFragment
import org.tokend.template.features.send.logic.CreatePaymentRequestUseCase
import org.tokend.template.features.send.model.PaymentRecipient
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.features.send.recipient.view.PaymentRecipientFragment
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.logic.FeeManager
import org.tokend.template.logic.wallet.WalletEventsListener
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.ProgressDialogFactory
import org.tokend.template.view.util.input.SoftInputUtil
import java.math.BigDecimal

class SendFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private val requiredAsset: String?
        get() = arguments?.getString(ASSET_EXTRA)

    private val allowToolbar: Boolean
        get() = arguments?.getBoolean(ALLOW_TOOLBAR_EXTRA) ?: true


    private var recipient: PaymentRecipient? = null
    private var amount: BigDecimal = BigDecimal.ZERO
    private var asset: String = ""
    private var description: String? = null

    private var isWaitingForTransferableAssets: Boolean = true


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_user_flow, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)
        toolbar.title = getString(R.string.send_title)
        if (!allowToolbar) appbar.visibility = View.GONE

        initSwipeRefresh()
        initErrorEmptyView()

        subscribeToBalances()

        update()
    }

    // region Init
    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { balancesRepository.update() }
    }

    private fun initErrorEmptyView() {
        error_empty_view.setEmptyDrawable(R.drawable.ic_send)
    }
    // endregion

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
                        .subscribe { loadingIndicator.setLoading(it, "balances") },
                balancesRepository.errorsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            if (isWaitingForTransferableAssets) {
                                toErrorView(it)
                            } else {
                                errorHandlerFactory.getDefault().handle(it)
                            }
                        }
        ).also { it.addTo(compositeDisposable) }
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            balancesRepository.updateIfNotFresh()
        } else {
            balancesRepository.update()
        }
    }

    private fun onBalancesUpdated() {
        val anyTransferableAssets = balancesRepository
                .itemsList
                .map(BalanceRecord::asset)
                .any(AssetRecord::isTransferable)

        if (anyTransferableAssets) {
            if (isWaitingForTransferableAssets) {
                isWaitingForTransferableAssets = false

                hideErrorOrEmptyView()
                toRecipientScreen()
            }
        } else {
            isWaitingForTransferableAssets = true
            toEmptyView()
        }
    }

    private fun toRecipientScreen() {
        val fragment = PaymentRecipientFragment()

        fragment
                .resultObservable
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribeBy(
                        onNext = this::onRecipientSelected,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
                .addTo(compositeDisposable)

        displayFragment(fragment, "recipient", null)
    }

    private fun onRecipientSelected(recipient: PaymentRecipient) {
        this.recipient = recipient
        toAmountScreen()
    }

    private fun toAmountScreen() {
        val recipient = recipient?.displayedValue!!

        val fragment = PaymentAmountAndDescriptionFragment.newInstance(recipient, requiredAsset)

        fragment
                .resultObservable
                .map { it as PaymentAmountAndDescription }
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribeBy(
                        onNext = this::onAmountEntered,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )

        displayFragment(fragment, "amount", true)
    }

    private fun onAmountEntered(result: PaymentAmountAndDescription) {
        this.amount = result.amount
        this.asset = result.assetCode
        this.description = result.description

        createAndConfirmPaymentRequest()
    }

    private var paymentRequestDisposable: Disposable? = null
    private fun createAndConfirmPaymentRequest() {
        val recipient = recipient ?: return

        val progress = ProgressDialogFactory.getTunedDialog(requireContext()).apply {
            setCanceledOnTouchOutside(true)
            setOnCancelListener {
                paymentRequestDisposable?.dispose()
            }
            setMessage(getString(R.string.loading_data))
        }

        paymentRequestDisposable?.dispose()
        paymentRequestDisposable = CreatePaymentRequestUseCase(
                recipient,
                amount,
                asset,
                description,
                walletInfoProvider,
                FeeManager(apiProvider),
                balancesRepository
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    progress.show()
                }
                .doOnEvent { _, _ ->
                    progress.hide()
                }
                .subscribeBy(
                        onSuccess = this::onPaymentRequestCreated,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
                .addTo(compositeDisposable)
    }

    private fun onPaymentRequestCreated(request: PaymentRequest) {
        Navigator
                .from(this)
                .openPaymentConfirmation(
                        PAYMENT_CONFIRMATION_REQUEST,
                        request
                )
    }

    private fun displayFragment(
            fragment: Fragment,
            tag: String,
            forward: Boolean?
    ) {
        childFragmentManager.beginTransaction()
                .setTransition(
                        when (forward) {
                            true -> FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                            false -> FragmentTransaction.TRANSIT_FRAGMENT_CLOSE
                            null -> FragmentTransaction.TRANSIT_NONE
                        }
                )
                .replace(R.id.fragment_container_layout, fragment)
                .addToBackStack(tag)
                .commit()
    }

    private fun clearScreensBackStack() {
        childFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    // region Error/empty
    private fun toEmptyView() {
        clearScreensBackStack()
        SoftInputUtil.hideSoftInput(requireActivity())
        error_empty_view.showEmpty(R.string.error_no_transferable_assets)
    }

    private fun toErrorView(e: Throwable) {
        clearScreensBackStack()
        SoftInputUtil.hideSoftInput(requireActivity())
        error_empty_view.showError(e, errorHandlerFactory.getDefault()) {
            update(force = true)
        }
    }

    private fun hideErrorOrEmptyView() {
        error_empty_view.hide()
    }
    // endregion

    override fun onBackPressed(): Boolean {
        return if (childFragmentManager.backStackEntryCount <= 1) {
            true
        } else {
            childFragmentManager.popBackStackImmediate()
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

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
        private const val ALLOW_TOOLBAR_EXTRA = "allow_toolbar"
        const val ID = 1118L
        val PAYMENT_CONFIRMATION_REQUEST = "confirm_payment".hashCode() and 0xffff

        fun newInstance(asset: String? = null, allowToolbar: Boolean): SendFragment {
            val fragment = SendFragment()
            fragment.arguments = Bundle().apply {
                putString(ASSET_EXTRA, asset)
                putBoolean(ALLOW_TOOLBAR_EXTRA, allowToolbar)
            }
            return fragment
        }
    }
}