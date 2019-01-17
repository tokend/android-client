package org.tokend.template.features.wallet

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
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
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.data.repository.transactions.TxRepository
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.adapter.history.TxHistoryAdapter
import org.tokend.template.view.adapter.history.TxHistoryItem
import org.tokend.template.view.util.HorizontalSwipesGestureDetector
import org.tokend.template.view.util.LoadingIndicatorManager
import java.lang.ref.WeakReference
import java.util.*

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
        initHistory()
        initSwipeRefresh()
        initSend()
        initHorizontalSwipesIfNeeded()

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
        txAdapter.amountFormatter = amountFormatter
        txAdapter.onItemClick { _, item ->
            item.source?.let { Navigator.openTransactionDetails(this.activity!!, it) }
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

    private fun initHorizontalSwipesIfNeeded() {
        if (!needAssetTabs) {
            return
        }

        val weakTabs = WeakReference(asset_tabs)

        val gestureDetector = GestureDetectorCompat(requireContext(), HorizontalSwipesGestureDetector(
                onSwipeToLeft = {
                    weakTabs.get()?.apply { selectedItemIndex++ }
                },
                onSwipeToRight = {
                    weakTabs.get()?.apply { selectedItemIndex-- }
                }
        ))

        touch_capture_layout.setTouchEventInterceptor(gestureDetector::onTouchEvent)
        swipe_refresh.setOnTouchListener { _, event ->
            if (error_empty_view.visibility == View.VISIBLE)
                gestureDetector.onTouchEvent(event)

            false
        }
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
                            history_list.resetBottomReachHandled()
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
        Collections.sort(assets, assetComparator)
        asset_tabs.setSimpleItems(assets, asset)
    }

    private fun displayBalance() {
        balancesRepository.itemsList
                .find { it.assetCode == asset }
                ?.let { balance ->
                    val available = balance.available
                    collapsing_toolbar.title = amountFormatter.formatAssetAmount(available, asset) +
                            " $asset"
                    val converted = balance.availableConverted
                    val conversionAsset = balance.conversionAssetCode
                    converted_balance_text_view.text =
                            amountFormatter.formatAssetAmount(converted, conversionAsset) +
                            " $conversionAsset"
                }
    }

    private fun displaySendIfNeeded() {
        (balancesRepository.itemsList
                .find { it.assetCode == asset }
                ?.asset
                ?.isTransferable == true)
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

    private fun onBalancesUpdated(balances: List<BalanceRecord>) {
        displayAssetTabs(balances.map { it.assetCode })
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

    override fun onDestroyView() {
        super.onDestroyView()
        history_list.clearOnScrollListeners()
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
