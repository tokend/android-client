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
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_order_book.*
import org.tokend.template.R
import org.tokend.template.data.model.AssetPairRecord
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.data.repository.orderbook.OrderBookRepository
import org.tokend.template.features.offers.CreateOfferDialog
import org.tokend.template.features.offers.logic.PrepareOfferUseCase
import org.tokend.template.features.trade.orderbook.view.adapter.OrderBookAdapter
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.logic.FeeManager
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.ProgressDialogFactory
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

        displayBalance()
        displayPrice()
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
            openOfferDialog(item)
        }

        sellAdapter.onItemClick { _, item ->
            openOfferDialog(item)
        }
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(true) }

        balance_app_bar.addOnOffsetChangedListener { _, verticalOffset ->
            swipe_refresh.isEnabled = verticalOffset == 0
        }
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
        val balances = balancesRepository.itemsList

        val firstBalance = balances.find { it.assetCode == assetPair.base }?.available
        val secondBalance = balances.find { it.assetCode == assetPair.quote }?.available

        balance_text_view.text = getString(R.string.template_balance_two_assets,
                amountFormatter.formatAssetAmount(firstBalance, assetPair.base),
                amountFormatter.formatAssetAmount(secondBalance, assetPair.quote))
    }
    // endregion


    private fun displayPrice() {
        price_text_view.text = getString(R.string.template_price_one_equals, assetPair.base,
                amountFormatter.formatAssetAmount(assetPair.price, assetPair.quote))
    }

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
        openOfferDialog(
                OfferRecord(
                        baseAssetCode = assetPair.base,
                        quoteAssetCode = assetPair.quote,
                        baseAmount = BigDecimal.ZERO,
                        price = assetPair.price,
                        isBuy = false
                )
        )
    }

    private fun openOfferDialog(offer: OfferRecord) {
        CreateOfferDialog.withArgs(offer, amountFormatter)
                .showDialog(this.childFragmentManager, "create_offer")
                .subscribe {
                    goToOfferConfirmation(it)
                }
                .addTo(compositeDisposable)
    }

    private fun goToOfferConfirmation(offer: OfferRecord) {
        val progress = ProgressDialogFactory.getTunedDialog(requireContext())

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
        private val CREATE_OFFER_REQUEST = "create_offer".hashCode() and 0xffff
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
