package org.tokend.template.features.trade.orderbook.view

import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_order_book.*
import org.tokend.template.R
import org.tokend.template.data.model.AssetPairRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.features.trade.orderbook.model.OrderBook
import org.tokend.template.features.trade.orderbook.model.OrderBookEntryRecord
import org.tokend.template.features.trade.orderbook.repository.OrderBookRepository
import org.tokend.template.features.trade.orderbook.view.adapter.OrderBookEntriesAdapter
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager
import java.math.BigDecimal

class OrderBookFragment : BaseFragment() {

    private lateinit var assetPair: AssetPairRecord

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private val orderBookRepository: OrderBookRepository
        get() = repositoryProvider.orderBook(assetPair.base, assetPair.quote)

    private val orderBook: OrderBook?
        get() = orderBookRepository.item

    private lateinit var buyAdapter: OrderBookEntriesAdapter
    private lateinit var sellAdapter: OrderBookEntriesAdapter

    private var isBottomSheetExpanded = false
    private lateinit var bottomSheet: BottomSheetBehavior<LinearLayout>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_order_book, container, false)
    }

    override fun onInitAllowed() {
        this.assetPair = arguments?.getSerializable(ASSET_PAIR_EXTRA) as? AssetPairRecord
                ?: return

        initLists()
        initSwipeRefresh()
        initFab()

        subscribeToBalances()
        subscribeToOrderBook()

        displayOrderBookHeaders()

        update()
    }

    // region Init

    private fun initLists() {
        buyAdapter = OrderBookEntriesAdapter(true, amountFormatter)
        sellAdapter = OrderBookEntriesAdapter(false, amountFormatter)

        buy_entries_recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = buyAdapter
        }

        sell_entries_recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sellAdapter
        }

        buyAdapter.onItemClick { _, item ->
            openOfferCreation(item.price)
        }

        sellAdapter.onItemClick { _, item ->
            openOfferCreation(item.price)
        }
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(true) }
    }

    private fun initFab() {
        add_offer_fab.setOnClickListener {
            openOfferCreation()
        }
    }
    // endregion

    // region Balances
    private var balancesDisposable: CompositeDisposable? = null

    private fun subscribeToBalances() {
        balancesDisposable?.dispose()
        balancesDisposable = CompositeDisposable(
                balancesRepository.itemsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe(),
                balancesRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { loadingIndicator.setLoading(it, "balances") }
        ).also { it.addTo(compositeDisposable) }
    }
    // endregion

    // region Order book
    private fun displayOrderBookHeaders() {
        bid_hint.text = getString(R.string.template_offer_bid_asset, assetPair.quote)
        ask_hint.text = getString(R.string.template_offer_ask_asset, assetPair.quote)
    }

    private var orderBookDisposable: CompositeDisposable? = null
    private fun subscribeToOrderBook() {
        orderBookDisposable?.dispose()
        orderBookDisposable = CompositeDisposable(
                orderBookRepository
                        .itemSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { displayOrderBook() },
                orderBookRepository
                        .loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { loadingIndicator.setLoading(it, "order_book") },
                orderBookRepository
                        .errorsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { errorHandlerFactory.getDefault().handle(it) }
        ).also { it.addTo(compositeDisposable) }
    }

    private fun displayOrderBook() {
        displayBuyEntries(orderBook?.buyEntries ?: emptyList())
        displaySellEntries(orderBook?.sellEntries ?: emptyList())
    }

    private fun displayBuyEntries(items: Collection<OrderBookEntryRecord>) {
        buyAdapter.setData(items)
        if (items.isEmpty() && !orderBookRepository.isNeverUpdated) {
            bids_empty_view.visibility = View.VISIBLE
        } else {
            bids_empty_view.visibility = View.INVISIBLE
        }
    }

    private fun displaySellEntries(items: Collection<OrderBookEntryRecord>) {
        sellAdapter.setData(items)
        if (items.isEmpty() && !orderBookRepository.isNeverUpdated) {
            asks_empty_view.visibility = View.VISIBLE
        } else {
            asks_empty_view.visibility = View.INVISIBLE
        }
    }
    // endregion

    private fun update(force: Boolean = false) {
        listOf(
                balancesRepository,
                orderBookRepository
        ).forEach {
            if (!force) {
                it.updateIfNotFresh()
            } else {
                it.update()
            }
        }
    }

    // region Offer creation
    private fun openOfferCreation(price: BigDecimal? = null) {
        val orderBook = this.orderBook ?: return

        Navigator
                .from(this)
                .openCreateOffer(
                        baseAssetCode = orderBook.baseAssetCode,
                        quoteAssetCode = orderBook.quoteAssetCode,
                        requiredPrice = price
                )
    }
    // endregion

    override fun onBackPressed(): Boolean {
        return if (isBottomSheetExpanded) {
            bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            false
        } else {
            bottomSheet.setBottomSheetCallback(null)
            super.onBackPressed()
        }
    }

    companion object {
        private const val ASSET_PAIR_EXTRA = "asset_pair"
        const val ID = 1115L

        fun newInstance(assetPair: AssetPairRecord): OrderBookFragment {
            val fragment = OrderBookFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(ASSET_PAIR_EXTRA, assetPair)
            }
            return fragment
        }
    }
}
