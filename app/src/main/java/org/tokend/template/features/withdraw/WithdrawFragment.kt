package org.tokend.template.features.withdraw

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
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
import kotlinx.android.synthetic.main.fragment_withdraw.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.R
import org.tokend.template.data.model.AssetRecord
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.features.amountscreen.model.AmountInputResult
import org.tokend.template.features.withdraw.amount.view.WithdrawAmountFragment
import org.tokend.template.features.withdraw.destination.view.WithdrawDestinationFragment
import org.tokend.template.features.withdraw.logic.CreateWithdrawalRequestUseCase
import org.tokend.template.features.withdraw.logic.WithdrawalAddressUtil
import org.tokend.template.features.withdraw.model.WithdrawalRequest
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
    private var asset: String = ""

    private var isWaitingForWithdrawableAssets: Boolean = true

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
        error_empty_view.background = ColorDrawable(
                ContextCompat.getColor(requireContext(), R.color.colorDefaultBackground)
        )
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
        val fragment = WithdrawAmountFragment.newInstance(requiredAsset)

        fragment
                .resultObservable
                .map { it as AmountInputResult }
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribeBy(
                        onNext = this::onAmountEntered,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )

        displayFragment(fragment, "amount", null)
    }

    private fun onAmountEntered(result: AmountInputResult) {
        this.amount = result.amount
        this.asset = result.assetCode

        toDestinationScreen()
    }

    private fun toDestinationScreen() {
        val amountToWithdraw = amountFormatter.formatAssetAmount(amount, asset)
        val fragment = WithdrawDestinationFragment.newInstance(amountToWithdraw)

        fragment
                .resultObservable
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribeBy(
                        onNext = this::onDestinationEntered,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
        displayFragment(fragment, "destination", true)
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
        val address = destination
                .trim()
                .let {
                    WithdrawalAddressUtil().extractAddressFromInvoice(it)
                            ?: it
                }

        val progress = ProgressDialogFactory.getTunedDialog(requireContext()).apply {
            setCanceledOnTouchOutside(true)
            setOnCancelListener {
                withdrawRequestDisposable?.dispose()
            }
            setMessage(getString(R.string.loading_data))
        }

        withdrawRequestDisposable?.dispose()
        withdrawRequestDisposable = CreateWithdrawalRequestUseCase(
                amount,
                asset,
                address,
                walletInfoProvider,
                repositoryProvider.balances(),
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
                            Navigator.from(this).openWithdrawalConfirmation(
                                    WITHDRAWAL_CONFIRMATION_REQUEST, request
                            )

                        },
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
                .addTo(compositeDisposable)
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
        childFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    // region Error/empty
    private fun toEmptyView() {
        clearScreensBackStack()
        SoftInputUtil.hideSoftInput(requireActivity())
        error_empty_view.showEmpty(R.string.error_no_withdrawable_assets)
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
                WITHDRAWAL_CONFIRMATION_REQUEST -> {
                    val confirmedRequest =
                            data?.getSerializableExtra(
                                    WithdrawalConfirmationActivity.WITHDRAWAL_REQUEST_EXTRA
                            ) as? WithdrawalRequest
                    if (confirmedRequest != null) {
                        (activity as? WalletEventsListener)
                                ?.onWithdrawalRequestConfirmed(confirmedRequest)
                    }
                }
            }
        }
    }

    companion object {
        private const val ASSET_EXTRA = "asset"
        const val ID = 1113L
        val WITHDRAWAL_CONFIRMATION_REQUEST = "confirm_withdrawal".hashCode() and 0xffff

        fun newInstance(asset: String? = null): WithdrawFragment {
            val fragment = WithdrawFragment()
            fragment.arguments = Bundle().apply {
                putString(ASSET_EXTRA, asset)
            }
            return fragment
        }
    }
}