package org.tokend.template.features.withdraw

import android.os.Bundle
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
import kotlinx.android.synthetic.main.fragment_user_flow.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.AssetRecord
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.repository.BalancesRepository
import org.tokend.template.extensions.withArguments
import org.tokend.template.features.amountscreen.model.AmountInputResult
import org.tokend.template.features.withdraw.amount.view.WithdrawAmountFragment
import org.tokend.template.features.withdraw.destination.view.WithdrawDestinationFragment
import org.tokend.template.features.withdraw.logic.CreateWithdrawalRequestUseCase
import org.tokend.template.features.withdraw.logic.WithdrawalAddressUtil
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.logic.FeeManager
import org.tokend.template.logic.WalletEventsListener
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.ProgressDialogFactory
import org.tokend.template.view.util.UserFlowFragmentDisplayer
import org.tokend.template.view.util.input.SoftInputUtil
import java.math.BigDecimal

class WithdrawFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private val requiredAsset: String?
        get() = arguments?.getString(ASSET_EXTRA)

    private var destinationAddress: String? = null
    private var amount: BigDecimal = BigDecimal.ZERO
    private var asset: Asset? = null

    private var isWaitingForWithdrawableAssets: Boolean = true

    private val fragmentDisplayer =
            UserFlowFragmentDisplayer(this, R.id.fragment_container_layout)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_user_flow, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)

        toolbar.title = getString(R.string.withdraw_title)

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
        error_empty_view.setEmptyDrawable(R.drawable.ic_withdraw)
    }
    // endregion

    // region Balances
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
                            if (isWaitingForWithdrawableAssets) {
                                toErrorView(it)
                            } else {
                                errorHandlerFactory.getDefault().handle(it)
                            }
                        }
        ).also { it.addTo(compositeDisposable) }
    }

    private fun onBalancesUpdated() {
        val anyTransferableAssets =
                balancesRepository
                        .itemsList
                        .map(BalanceRecord::asset)
                        .any(AssetRecord::isWithdrawable)

        if (anyTransferableAssets) {
            if (isWaitingForWithdrawableAssets) {
                isWaitingForWithdrawableAssets = false

                hideErrorOrEmptyView()
                toAmountScreen()
            }
        } else {
            isWaitingForWithdrawableAssets = true
            toEmptyView()
        }
    }
    // endregion

    private fun toAmountScreen() {
        val fragment = WithdrawAmountFragment.newInstance(
                WithdrawAmountFragment.getBundle(requiredAsset)
        )

        fragment
                .resultObservable
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribeBy(
                        onNext = this::onAmountEntered,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )

        fragmentDisplayer.display(fragment, "amount", null)
    }

    private fun onAmountEntered(result: AmountInputResult) {
        this.amount = result.amount
        this.asset = result.asset

        toDestinationScreen()
    }

    private fun toDestinationScreen() {
        val asset = this.asset ?: return
        val amountToWithdraw = amountFormatter.formatAssetAmount(amount, asset)
        val fragment = WithdrawDestinationFragment.newInstance(
                WithdrawDestinationFragment.getBundle(amountToWithdraw)
        )

        fragment
                .resultObservable
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribeBy(
                        onNext = this::onDestinationEntered,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
        fragmentDisplayer.display(fragment, "destination", true)
    }

    private fun onDestinationEntered(destination: String) {
        this.destinationAddress = destination

        createAndConfirmWithdrawRequest()
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            balancesRepository.updateIfNotFresh()
        } else {
            balancesRepository.update()
        }
    }

    private var withdrawRequestDisposable: Disposable? = null
    private fun createAndConfirmWithdrawRequest() {
        val destination = this.destinationAddress ?: return
        val asset = this.asset ?: return
        val address = destination
                .trim()
                .let {
                    WithdrawalAddressUtil().extractAddressFromInvoice(it)
                            ?: it
                }

        val progress = ProgressDialogFactory.getDialog(
                requireContext(),
                R.string.loading_data
        ) {
            withdrawRequestDisposable?.dispose()
        }

        withdrawRequestDisposable?.dispose()
        withdrawRequestDisposable = CreateWithdrawalRequestUseCase(
                amount,
                asset,
                address,
                walletInfoProvider,
                balancesRepository,
                FeeManager(apiProvider)
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
                        onSuccess = { request ->
                            Navigator.from(this)
                                    .openWithdrawalConfirmation(request)
                                    .addTo(activityRequestsBag)
                                    .doOnSuccess {
                                        (activity as? WalletEventsListener)
                                                ?.onWithdrawalRequestConfirmed(request)
                                    }
                        },
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
                .addTo(compositeDisposable)
    }

    // region Error/empty
    private fun toEmptyView() {
        fragmentDisplayer.clearBackStack()
        SoftInputUtil.hideSoftInput(requireActivity())
        error_empty_view.showEmpty(R.string.error_no_withdrawable_assets)
    }

    private fun toErrorView(e: Throwable) {
        fragmentDisplayer.clearBackStack()
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
        return !fragmentDisplayer.tryPopBackStack()
    }

    companion object {
        private const val ASSET_EXTRA = "asset"
        val ID = "withdraw".hashCode().toLong()

        fun newInstance(bundle: Bundle): WithdrawFragment =
                WithdrawFragment().withArguments(bundle)

        fun getBundle(assetCode: String? = null) = Bundle().apply {
            putString(ASSET_EXTRA, assetCode)
        }
    }
}