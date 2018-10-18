package org.tokend.template.base.fragments


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.collapsing_balance_appbar.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.base.model.operations.*
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.base.activities.tx_details.*
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.template.base.logic.repository.transactions.TxRepository
import org.tokend.template.base.view.adapter.history.TxHistoryAdapter
import org.tokend.template.base.view.adapter.history.TxHistoryItem
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.extensions.BalanceDetails
import org.tokend.template.extensions.isTransferable
import org.tokend.template.features.invest.activities.InvestmentDetailsActivity
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers

class WalletFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private lateinit var balancesRepository: BalancesRepository
    private val txRepository: TxRepository
        get() = repositoryProvider.transactions(asset)

    private val needAssetTabs: Boolean
        get() = arguments?.getBoolean(NEED_TABS_EXTRA) == true

    private var asset: String = ""
        set(value) {
            field = value
            onAssetChanged()
        }

    private val txAdapter = TxHistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        balancesRepository = repositoryProvider.balances()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)

        initAssetTabs()
        initBalance()
        initHistory()
        initSwipeRefresh()
        initSend()

        arguments?.getString(ASSET_EXTRA)?.let { requiredAsset ->
            asset = requiredAsset
        }

        subscribeToBalances()
    }

    private fun initSend() {
        send_fab.onClick {
            Navigator.openSend(this, asset, SEND_REQUEST)
        }
    }

    // region Init
    private fun initAssetTabs() {
        asset_tabs.onItemSelected {
            asset = it.text
        }

        if (!needAssetTabs) {
            asset_tabs.visibility = View.GONE
        } else {
            asset_tabs.visibility = View.VISIBLE

            val tabsOffset = requireContext().dip(24)
            collapsing_toolbar.layoutParams =
                    collapsing_toolbar.layoutParams.apply {
                        height -= 2 * tabsOffset
                    }
        }
    }

    private fun initBalance() {
        converted_balance_text_view.text = "0 USD"
    }

    private val hideFabScrollListener =
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                    if (dy > 2) {
                        send_fab.hide()
                    } else if (dy < -2 && send_fab.isEnabled) {
                        send_fab.show()
                    }
                }
            }

    private fun initHistory() {
        txAdapter.onItemClick { _, item ->
            item.source?.let { openTransactionDetails(it) }
        }

        error_empty_view.setPadding(0, 0, 0,
                resources.getDimensionPixelSize(R.dimen.quadra_margin))
        error_empty_view.observeAdapter(txAdapter, R.string.no_transaction_history)
        error_empty_view.setEmptyViewDenial { txRepository.isNeverUpdated }

        history_list.adapter = txAdapter
        history_list.layoutManager = LinearLayoutManager(context!!)
        history_list.addOnScrollListener(hideFabScrollListener)

        history_list.listenBottomReach({ txAdapter.getDataItemCount() }) {
            txRepository.loadMore() || txRepository.noMoreItems
        }
    }


    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }
    // endregion

    // region Subscriptions
    private fun subscribeToBalances() {
        balancesRepository.itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    onBalancesUpdated(it)
                }
                .addTo(compositeDisposable)
        balancesRepository.loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    loadingIndicator.setLoading(it, "balances")
                }
                .addTo(compositeDisposable)
        balancesRepository.errorsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    errorHandlerFactory.getDefault().handle(it)
                }
                .addTo(compositeDisposable)
    }

    private var transactionsDisposable: Disposable? = null
    private var transactionsLoadingDisposable: Disposable? = null
    private var transactionsErrorsDisposable: Disposable? = null
    private fun subscribeToTransactions() {
        transactionsDisposable?.dispose()
        transactionsDisposable =
                txRepository.itemsSubject
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            txAdapter.setData(it.map {
                                TxHistoryItem.fromTransaction(it)
                            })
                            history_list.resetPagination()
                        }
                        .addTo(compositeDisposable)

        transactionsLoadingDisposable?.dispose()
        transactionsLoadingDisposable =
                txRepository.loadingSubject
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { loading ->
                            if (loading) {
                                if (txRepository.isOnFirstPage) {
                                    loadingIndicator.show("transactions")
                                } else {
                                    txAdapter.showLoadingFooter()
                                }
                            } else {
                                loadingIndicator.hide("transactions")
                                txAdapter.hideLoadingFooter()
                            }
                        }
                        .addTo(compositeDisposable)

        transactionsErrorsDisposable?.dispose()
        transactionsErrorsDisposable =
                txRepository.errorsSubject
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { error ->
                            if (!txAdapter.hasData) {
                                error_empty_view.showError(error, errorHandlerFactory.getDefault()) {
                                    update(true)
                                }
                            } else {
                                errorHandlerFactory.getDefault().handle(error)
                            }
                        }
                        .addTo(compositeDisposable)
    }
    // endregion

    // region Display
    private fun displayAssetTabs(assets: List<String>) {
        asset_tabs.setSimpleItems(assets, asset)
    }

    private fun displayBalance() {
        balancesRepository.itemsSubject.value
                .find { it.asset == asset }
                ?.let { balanceItem ->
                    val balance = balanceItem.balance
                    collapsing_toolbar.title = AmountFormatter.formatAssetAmount(balance, asset) +
                            " $asset"
                    val converted = balanceItem.convertedBalance
                    val conversionAsset = balanceItem.conversionAsset
                    converted_balance_text_view.text =
                            AmountFormatter.formatAssetAmount(converted, conversionAsset) +
                            " $conversionAsset"
                }
    }

    private fun displaySendIfNeeded() {
        (balancesRepository.itemsSubject.value
                .find { it.asset == asset }
                ?.assetDetails
                ?.isTransferable() == true)
                .let { isTransferable ->
                    if (!isTransferable || !BuildConfig.IS_SEND_ALLOWED) {
                        send_fab.hide()
                        send_fab.isEnabled = false
                    } else {
                        send_fab.show()
                        send_fab.isEnabled = true
                    }
                }
    }
    // endregion

    private fun update(force: Boolean = false) {
        if (!force) {
            balancesRepository.updateIfNotFresh()
            txRepository.updateIfNotFresh()
        } else {
            balancesRepository.update()
            txRepository.update()
        }
    }

    private fun onBalancesUpdated(balances: List<BalanceDetails>) {
        displayAssetTabs(balances.map { it.asset })
        displayBalance()
        displaySendIfNeeded()
    }

    private fun onAssetChanged() {
        displayBalance()
        subscribeToTransactions()
        date_text_switcher.init(history_list, txAdapter)
        history_list.scrollToPosition(0)
        displaySendIfNeeded()
        update()
    }

    private fun openTransactionDetails(tx: TransferOperation) {
        when (tx) {
            is PaymentOperation ->
                TxDetailsActivity
                        .start<PaymentDetailsActivity, PaymentOperation>(activity!!, tx)
            is IssuanceOperation ->
                TxDetailsActivity
                        .start<DepositDetailsActivity, IssuanceOperation>(activity!!, tx)
            is WithdrawalOperation ->
                TxDetailsActivity
                        .start<WithdrawalDetailsActivity, WithdrawalOperation>(activity!!, tx)
            is InvestmentOperation ->
                TxDetailsActivity
                        .start<InvestmentDetailsActivity, InvestmentOperation>(activity!!, tx)
            is OfferMatchOperation ->
                TxDetailsActivity
                        .start<OfferMatchDetailsActivity, OfferMatchOperation>(activity!!, tx)
            else ->
                (tx as? BaseTransferOperation)?.let {
                    TxDetailsActivity
                            .start<UnknownTxDetailsActivity, BaseTransferOperation>(activity!!, it)
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                SEND_REQUEST -> {
                    update()
                }
            }
        }
    }

    companion object {
        private const val ASSET_EXTRA = "asset"
        private const val NEED_TABS_EXTRA = "need_tabs"
        private val SEND_REQUEST = "send".hashCode() and 0xffff
        const val ID = 1111L

        fun newInstance(asset: String? = null, needTabs: Boolean = true): WalletFragment {
            val fragment = WalletFragment()
            fragment.arguments = Bundle().apply {
                putString(ASSET_EXTRA, asset)
                putBoolean(NEED_TABS_EXTRA, needTabs)
            }
            return fragment
        }
    }
}
