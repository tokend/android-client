package org.tokend.template.features.trade

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_trade.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.layout_asset_chart_sheet.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.assets.model.AssetPair
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.template.R
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.logic.FeeManager
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.data.repository.pairs.AssetPairsRepository
import org.tokend.template.view.picker.PickerItem
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.extensions.isTradeable
import org.tokend.template.extensions.toSingle
import org.tokend.template.features.trade.adapter.OrderBookAdapter
import org.tokend.template.features.offers.logic.PrepareOfferUseCase
import org.tokend.template.data.repository.orderbook.OrderBookRepository
import org.tokend.template.features.offers.CreateOfferDialog
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import java.math.BigDecimal


class TradeFragment : BaseFragment(), ToolbarProvider {

    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val pairs = mutableListOf<AssetPair>()
    private var currentPair: AssetPair =
            AssetPair("", "", BigDecimal.ONE, BigDecimal.ONE)
        set(value) {
            field = value
            onPairChanged()
        }

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()
    private val assetPairsRepository: AssetPairsRepository
        get() = repositoryProvider.assetPairs()
    private val buyRepository: OrderBookRepository
        get() = repositoryProvider.orderBook(currentPair.base, currentPair.quote, true)
    private val sellRepository: OrderBookRepository
        get() = repositoryProvider.orderBook(currentPair.base, currentPair.quote, false)

    private val buyAdapter = OrderBookAdapter(true)
    private val sellAdapter = OrderBookAdapter(false)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trade, container, false)
    }

    override fun onInitAllowed() {
        initToolbar()
        initLists()
        initPairSelection()
        initSwipeRefresh()
        initChart()

        subscribeToPairs()
        subscribeToBalances()

        update()
    }

    // region Init
    private fun initToolbar() {
        toolbarSubject.onNext(toolbar)
        toolbar.title = context?.getString(R.string.trade_title)
    }

    private fun initLists() {
        buy_offers_recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = buyAdapter
        }

        sell_offers_recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sellAdapter
        }

        buyAdapter.onItemClick { _, item ->
            openOfferDialog(item)
        }

        sellAdapter.onItemClick { _, item ->
            openOfferDialog(item)
        }
    }

    private fun initPairSelection() {
        pairs_tabs.onItemSelected {
            (it.tag as? AssetPair)?.let { currentPair = it }
        }
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(true) }

        balance_app_bar.addOnOffsetChangedListener { _, verticalOffset ->
            swipe_refresh.isEnabled = verticalOffset == 0
        }
    }

    private fun initChart() {
        pair_chart.apply {
            post {
                this.valueTextSizePx = this@TradeFragment.context!!.resources.getDimension(R.dimen.text_size_heading_large)
                val asset = currentPair.quote

                visibility = View.VISIBLE
                this.asset = asset
                valueHint = getString(R.string.deployed_hint)

                applyTouchHook(root_layout)

                peek.onClick {
                    val bottomSheet = BottomSheetBehavior.from(bottom_sheet)

                    val state = when (bottomSheet.state) {
                        BottomSheetBehavior.STATE_COLLAPSED -> BottomSheetBehavior.STATE_EXPANDED
                        else -> BottomSheetBehavior.STATE_COLLAPSED
                    }

                    bottomSheet.state = state
                }

                BottomSheetBehavior.from(bottom_sheet).setBottomSheetCallback(
                        object : BottomSheetBehavior.BottomSheetCallback() {
                            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                                peek_image.rotation = 180 * slideOffset
                            }

                            override fun onStateChanged(bottomSheet: View, newState: Int) {
                                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                                    chart_sheet_elevation_view.visibility = View.GONE
                                } else {
                                    chart_sheet_elevation_view.visibility = View.VISIBLE
                                }
                            }
                        }
                )
            }
        }
    }


    private fun initMenu() {
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu_trade)

        if (pairs.isEmpty()) {
            toolbar.menu.findItem(R.id.add_order)?.isVisible = false
        }

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.add_order -> {
                    createOffer()
                    true
                }
                R.id.pending_offers -> {
                    Navigator.openPendingOffers(this, CANCEL_OFFER_REQUEST)
                    true
                }
                else -> false
            }
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
                        .subscribe {
                            onBalancesUpdated()
                        },
                balancesRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { loadingIndicator.setLoading(it, "balances") }
        ).also { it.addTo(compositeDisposable) }
    }

    private fun onBalancesUpdated() {
        displayBalance()
    }

    private fun displayBalance() {
        val balances = balancesRepository.itemsSubject.value

        val firstBalance = balances.find { it.asset == currentPair.base }?.balance
        val secondBalance = balances.find { it.asset == currentPair.quote }?.balance

        balance_text_view.text = getString(R.string.template_balance_two_assets,
                AmountFormatter.formatAssetAmount(firstBalance), currentPair.base,
                AmountFormatter.formatAssetAmount(secondBalance), currentPair.quote)
    }
// endregion

    // region Pairs
    private var assetPairsDisposable: CompositeDisposable? = null

    private fun subscribeToPairs() {
        assetPairsDisposable?.dispose()
        assetPairsDisposable = CompositeDisposable(
                assetPairsRepository.itemsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            onNewPairs(it)
                        },
                assetPairsRepository.errorsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { error ->
                            if (assetPairsRepository.isNeverUpdated) {
                                error_empty_view.showError(error, errorHandlerFactory.getDefault()) {
                                    update(true)
                                }
                            } else {
                                errorHandlerFactory.getDefault().handle(error)
                            }
                        },
                assetPairsRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { loadingIndicator.setLoading(it, "pairs") }
        ).also { it.addTo(compositeDisposable) }
    }

    private fun onNewPairs(newPairs: List<AssetPair>) {
        pairs.clear()
        pairs.addAll(
                newPairs
                        .asSequence()
                        .filter { it.isTradeable() }
                        .sortedBy { it.base }
                        .toList()
        )

        displayPairs()
        displayPrice()
    }

    private fun displayPairs() {
        initMenu()

        if (pairs.isEmpty()) {
            pairs_tabs.visibility = View.GONE
            bottom_sheet.visibility = View.GONE
            error_empty_view.showEmpty(
                    if (assetPairsRepository.isNeverUpdated)
                        ""
                    else
                        getString(R.string.error_no_tradeable_pairs)
            )
            return
        } else {
            error_empty_view.hide()
            bottom_sheet.visibility = View.VISIBLE
            pairs_tabs.visibility = View.VISIBLE
        }

        pairs_tabs.setItems(
                pairs.map {
                    val text = getString(R.string.template_asset_pair,
                            it.base, it.quote)
                    PickerItem(text, it)
                }
        )
    }


    private fun onPairChanged() {
        pair_chart.asset = currentPair.quote
        pair_chart.total = currentPair.price
        pair_chart.valueHint = getString(R.string.asset_price_for_one_part, currentPair.base)

        displayOrderBookHeaders()
        displayBalance()
        displayPrice()

        subscribeToOrderBook()

        updateChart()
        updateOrderBook()
    }

    private fun displayPrice() {
        price_text_view.text = getString(R.string.template_price_one_equals, currentPair.base,
                AmountFormatter.formatAssetAmount(currentPair.price), currentPair.quote)
    }
// endregion

    // region Order book
    private fun displayOrderBookHeaders() {
        bid_heading.text = getString(R.string.template_buy_bid_asset, currentPair.base)
        ask_heading.text = getString(R.string.template_sell_ask_asset, currentPair.base)

        listOf(buy_amount_hint, sell_amount_hint).forEach {
            it.text = getString(R.string.template_offer_amount_asset, currentPair.base)
        }
        listOf(buy_price_hint, sell_price_hint).forEach {
            it.text = getString(R.string.template_offer_price_asset, currentPair.quote)
        }

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

    private var chartDisposable: Disposable? = null

    private fun updateChart() {
        chartDisposable?.dispose()
        chartDisposable = apiProvider.getApi()
                .assets
                .getChart(currentPair.base, currentPair.quote)
                .toSingle()
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    pair_chart.isLoading = true
                }
                .doOnEvent { _, _ ->
                    pair_chart.isLoading = false
                }
                .subscribeBy(
                        onSuccess = {
                            pair_chart.post {
                                pair_chart.data = it
                            }
                        },
                        onError = {
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
                .addTo(compositeDisposable)
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


    private fun displayBuyItems(items: Collection<Offer>) {
        buyAdapter.setData(items)
        if (items.isEmpty() && !buyRepository.isNeverUpdated) {
            bids_empty_view.visibility = View.VISIBLE
        } else {
            bids_empty_view.visibility = View.INVISIBLE
        }
    }

    private fun displaySellItems(items: Collection<Offer>) {
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
                balancesRepository, assetPairsRepository
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
        openOfferDialog(
                Offer(
                        baseAsset = currentPair.base,
                        quoteAsset = currentPair.quote,
                        price = currentPair.price
                )
        )
    }

    private fun openOfferDialog(offer: Offer) {
        CreateOfferDialog.withArgs(offer)
                .showDialog(this.childFragmentManager, "create_offer")
                .subscribe {
                    goToOfferConfirmation(it)
                }
                .addTo(compositeDisposable)
    }

    private fun goToOfferConfirmation(offer: Offer) {
        PrepareOfferUseCase(
                offer,
                walletInfoProvider,
                FeeManager(apiProvider)
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe { progress.show() }
                .doOnEvent { _, _ -> progress.hide() }
                .subscribeBy(
                        onSuccess = { completedOffer ->
                            Navigator.openOfferConfirmation(this,
                                    CREATE_OFFER_REQUEST, completedOffer)
                        },
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
                .addTo(compositeDisposable)
    }
// endregion

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CANCEL_OFFER_REQUEST -> {
                    update()
                }
            }
        }
    }

    companion object {
        private val CREATE_OFFER_REQUEST = "create_offer".hashCode() and 0xffff
        private val CANCEL_OFFER_REQUEST = "cancel_offer".hashCode() and 0xffff
        const val ID = 1115L
    }
}
