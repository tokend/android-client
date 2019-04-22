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
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.data.repository.orderbook.OrderBookRepository
import org.tokend.template.features.trade.orderbook.view.adapter.OrderBookAdapter
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
    private val buyRepository: OrderBookRepository
        get() = repositoryProvider.orderBook(assetPair.base, assetPair.quote, true)
    private val sellRepository: OrderBookRepository
        get() = repositoryProvider.orderBook(assetPair.base, assetPair.quote, false)

    private val buyAdapter = OrderBookAdapter(true)
    private val sellAdapter = OrderBookAdapter(false)

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
        buyAdapter.amountFormatter = amountFormatter
        sellAdapter.amountFormatter = amountFormatter

        buy_offers_recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = buyAdapter
        }

        sell_offers_recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sellAdapter
        }

        buyAdapter.onItemClick { _, item ->
            Navigator.from(this).openCreateOffer(item)
        }

        sellAdapter.onItemClick { _, item ->
            Navigator.from(this).openCreateOffer(item)
        }
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(true) }
    }

    private fun initFab() {
        add_offer_fab.setOnClickListener {
            createOffer()
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

    private fun updateOrderBook(force: Boolean = false) {
        if (force) {
            buyRepository.update()
            sellRepository.update()
        } else {
            buyRepository.updateIfNotFresh()
            sellRepository.updateIfNotFresh()
        }
    }

    private var orderBookDisposable: CompositeDisposable? = null
    private fun subscribeToOrderBook() {
        orderBookDisposable?.dispose()
        orderBookDisposable = CompositeDisposable(
                // region Items
                buyRepository.itemsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            displayBuyItems(it)
                        },
                sellRepository.itemsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            displaySellItems(it)
                        },
                // endregion

                // region Loading
                buyRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { isLoading ->
                            //loadingIndicator.setLoading(it, "buy")

                            if (isLoading) {
                                bids_empty_view.visibility = View.INVISIBLE
                                buyAdapter.showLoadingFooter()
                            } else {
                                buyAdapter.hideLoadingFooter()
                            }
                        },

                sellRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { isLoading ->
                            //                            loadingIndicator.setLoading(it, "sell")
                            if (isLoading) {
                                asks_empty_view.visibility = View.INVISIBLE
                                sellAdapter.showLoadingFooter()
                            } else {
                                sellAdapter.hideLoadingFooter()
                            }
                        }
                // endregion
        ).also { it.addTo(compositeDisposable) }
    }

    private fun displayBuyItems(items: Collection<OfferRecord>) {
        buyAdapter.setData(items)
        if (items.isEmpty() && !buyRepository.isNeverUpdated) {
            bids_empty_view.visibility = View.VISIBLE
        } else {
            bids_empty_view.visibility = View.INVISIBLE
        }
    }

    private fun displaySellItems(items: Collection<OfferRecord>) {
        sellAdapter.setData(items)
        if (items.isEmpty() && !sellRepository.isNeverUpdated) {
            asks_empty_view.visibility = View.VISIBLE
        } else {
            asks_empty_view.visibility = View.INVISIBLE
        }
    }
    // endregion

    private fun update(force: Boolean = false) {
        listOf(
                balancesRepository
        ).forEach {
            if (!force) {
                it.updateIfNotFresh()
            } else {
                it.update()
            }
        }
        updateOrderBook(force)
    }

    // region Offer creation
    private fun createOffer() {
        Navigator.from(this).openCreateOffer(
                OfferRecord(
                        baseAssetCode = assetPair.base,
                        quoteAssetCode = assetPair.quote,
                        baseAmount = BigDecimal.ZERO,
                        price = assetPair.price,
                        isBuy = false
                )
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
