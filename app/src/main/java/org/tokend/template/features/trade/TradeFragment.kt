package org.tokend.template.features.trade

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.trello.rxlifecycle2.android.FragmentEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_trade.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.sdk.api.models.AssetPair
import org.tokend.sdk.api.models.Offer
import org.tokend.template.R
import org.tokend.template.base.fragments.BaseFragment
import org.tokend.template.base.fragments.ToolbarProvider
import org.tokend.template.base.logic.FeeManager
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.template.base.view.picker.PickerItem
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.extensions.isTradeable
import org.tokend.template.features.trade.adapter.OrderBookAdapter
import org.tokend.template.features.trade.repository.order_book.OrderBookRepository
import org.tokend.template.base.logic.repository.pairs.AssetPairsRepository
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.error_handlers.ErrorHandlerFactory
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

    private val buyAdapter = OrderBookAdapter()
    private val sellAdapter = OrderBookAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trade, container, false)
    }

    override fun onInitAllowed() {
        initToolbar()
        initLists()
        initPairSelection()
        initSwipeRefresh()

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
                        .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                        .subscribe {
                            onBalancesUpdated()
                        },
                balancesRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                        .subscribe { loadingIndicator.setLoading(it, "balances") }
        )
    }

    private fun onBalancesUpdated() {
        displayBalance()
    }

    private fun displayBalance() {
        balance_card.radius = 0f

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
                        .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                        .subscribe {
                            onNewPairs(it)
                        },
                assetPairsRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                        .subscribe { loadingIndicator.setLoading(it, "pairs") }
        )
    }

    private fun onNewPairs(newPairs: List<AssetPair>) {
        pairs.clear()
        pairs.addAll(
                newPairs
                        .filter { it.isTradeable() }
                        .sortedBy { it.base }
        )

        displayPairs()
        displayPrice()
    }

    private fun displayPairs() {
        initMenu()

        if (pairs.isEmpty()) {
            pairs_tabs.visibility = View.GONE
            error_empty_view.showEmpty(
                    if (assetPairsRepository.isNeverUpdated)
                        ""
                    else
                        getString(R.string.error_no_tradeable_pairs)
            )
            return
        } else {
            pairs_tabs.visibility = View.VISIBLE
            error_empty_view.hide()
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
        displayOrderBookHeaders()
        displayBalance()
        displayPrice()

        subscribeToOrderBook()
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

    private var orderBookDisposable: CompositeDisposable? = null
    private fun subscribeToOrderBook() {
        orderBookDisposable?.dispose()
        orderBookDisposable = CompositeDisposable(
                // region Items
                buyRepository.itemsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                        .subscribe {
                            displayBuyItems(it)
                        },
                sellRepository.itemsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                        .subscribe {
                            displaySellItems(it)
                        },
                // endregion

                // region Loading
                buyRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
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
                        .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
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
        )
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
        val dialog = CreateOfferDialog.withArgs(offer)
        dialog.showDialog(this.childFragmentManager, "create_offer")
                .subscribe {
                    goToOfferConfirmation(it)
                }
    }

    private fun goToOfferConfirmation(offer: Offer) {
        getCurrentAccountId()
                .flatMap { accountId ->
                    FeeManager(apiProvider).getOfferFee(accountId,
                            offer.quoteAsset, offer.quoteAmount)
                }
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                .doOnSubscribe { progress.show() }
                .doOnEvent { _, _ -> progress.hide() }
                .subscribeBy(
                        onSuccess = { offerFee ->
                            offer.fee = offerFee.percent

                            Navigator.openOfferConfirmation(this,
                                    CREATE_OFFER_REQUEST, offer)
                        },
                        onError = { ErrorHandlerFactory.getDefault().handle(it) }
                )
    }

    private fun getCurrentAccountId(): Single<String> {
        return walletInfoProvider.getWalletInfo()?.accountId.toMaybe()
                .switchIfEmpty(Single.error(
                        IllegalStateException("Cannot obtain current account ID")
                ))
    }
    // endregion

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CREATE_OFFER_REQUEST, CANCEL_OFFER_REQUEST -> {
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
