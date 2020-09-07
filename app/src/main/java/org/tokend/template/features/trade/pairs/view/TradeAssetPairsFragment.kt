package org.tokend.template.features.trade.pairs.view

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_trade_asset_pairs.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.features.trade.pairs.model.AssetPairRecord
import org.tokend.template.features.trade.pairs.repository.AssetPairsRepository
import org.tokend.template.features.trade.pairs.view.adapter.AssetPairItemsAdapter
import org.tokend.template.features.trade.pairs.view.adapter.AssetPairListItem
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.SearchUtil
import org.tokend.template.util.navigation.Navigator
import org.tokend.template.view.picker.PickerItem
import org.tokend.template.view.util.*

class TradeAssetPairsFragment : BaseFragment(), ToolbarProvider {

    private val assetPairsRepository: AssetPairsRepository
        get() = repositoryProvider.assetPairs()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create()

    private var searchItem: MenuItem? = null

    private lateinit var pairsAdapter: AssetPairItemsAdapter
    private lateinit var layoutManager: GridLayoutManager

    private val comparator = Comparator<AssetPairListItem> { o1, o2 ->
        assetCodeComparator.compare(o1.baseAsset.code, o2.baseAsset.code)
    }

    private var filter: String? = null
        set(value) {
            if (value != field) {
                field = value
                onFilterChanged()
            }
        }

    private var quoteAsset: String? = null
        set(value) {
            if (value != field) {
                field = value
                onQuoteChanged()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trade_asset_pairs, container, false)
    }

    override fun onInitAllowed() {
        initToolbar()
        initSwipeRefresh()
        initList()
        initHorizontalSwipes()

        subscribeToAssetPairs()

        update()
    }

    // region Init
    private fun initToolbar() {
        toolbarSubject.onNext(toolbar)
        toolbar.title = context?.getString(R.string.trade_title)

        initMenu()
        initQuoteTabs()
    }

    private fun initMenu() {
        toolbar.inflateMenu(R.menu.trade_asset_pairs)

        val menu = toolbar.menu


        try {
            val searchItem = menu?.findItem(R.id.search)!!

            val searchManager = MenuSearchViewManager(searchItem, toolbar, compositeDisposable)

            searchManager.queryHint = getString(R.string.search)
            searchManager
                    .queryChanges
                    .compose(ObservableTransformers.defaultSchedulers())
                    .subscribe { newValue ->
                        filter = newValue.takeIf { it.isNotEmpty() }
                    }
                    .addTo(compositeDisposable)

            this.searchItem = searchItem
        } catch (e: Exception) {
            e.printStackTrace()
        }

        menu.findItem(R.id.pending_offers)?.setOnMenuItemClickListener {
            Navigator.from(this).openPendingOffers(false)
            true
        }
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.accent))
        swipe_refresh.setOnRefreshListener { update(true) }
    }

    private fun initList() {
        layoutManager = GridLayoutManager(requireContext(), 1)
        pairsAdapter = AssetPairItemsAdapter(amountFormatter)
        updateListColumnsCount()

        asset_pairs_recycler_view.layoutManager = layoutManager
        asset_pairs_recycler_view.adapter = pairsAdapter
        (asset_pairs_recycler_view.itemAnimator as? SimpleItemAnimator)
                ?.supportsChangeAnimations = false

        error_empty_view.setEmptyDrawable(R.drawable.ic_trade)
        error_empty_view.observeAdapter(pairsAdapter, R.string.error_no_tradeable_pairs)
        error_empty_view.setEmptyViewDenial { assetPairsRepository.isNeverUpdated }

        pairsAdapter.registerAdapterDataObserver(
                ScrollOnTopItemUpdateAdapterObserver(asset_pairs_recycler_view)
        )
        pairsAdapter.onItemClick { _, item ->
            item.source?.let { assetPair ->
                Navigator.from(this).openTrade(assetPair)
            }
        }
    }

    private fun initQuoteTabs() {
        pairs_tabs.onItemSelected {
            (it.tag as? String)?.let { quote ->
                quoteAsset = quote
            }
        }
    }

    private fun initHorizontalSwipes() {
        val gestureDetector = GestureDetectorCompat(requireContext(), HorizontalSwipesGestureDetector(
                onSwipeToLeft = {
                    pairs_tabs.apply { selectedItemIndex++ }
                },
                onSwipeToRight = {
                    pairs_tabs.apply { selectedItemIndex-- }
                }
        ))

        swipe_refresh.setTouchEventInterceptor { motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
            false
        }
    }
    // endregion

    private fun subscribeToAssetPairs() {
        assetPairsRepository
                .loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { loadingIndicator.setLoading(it) }
                .addTo(compositeDisposable)

        assetPairsRepository
                .errorsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { error ->
                    if (!pairsAdapter.hasData) {
                        error_empty_view.showError(error, errorHandlerFactory.getDefault()) {
                            update(true)
                        }
                    } else {
                        errorHandlerFactory.getDefault().handle(error)
                    }
                }
                .addTo(compositeDisposable)

        assetPairsRepository
                .itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { displayQuotes() }
                .addTo(compositeDisposable)
    }

    private fun onFilterChanged() {
        displayPairs()
    }

    private fun onQuoteChanged() {
        displayPairs()
    }

    private fun displayPairs() {
        val items = assetPairsRepository
                .itemsList
                .filter { it.isTradeable() && it.quote.code == quoteAsset }
                .map(::AssetPairListItem)
                .sortedWith(comparator)
                .let { items ->
                    filter?.let {
                        items.filter { item ->
                            SearchUtil.isMatchGeneralCondition(
                                    it,
                                    item.baseAsset.code
                            )
                        }
                    } ?: items
                }

        pairsAdapter.setData(items)
    }

    private fun displayQuotes() {
        val quotes = assetPairsRepository
                .itemsList
                .filter { it.isTradeable() }
                .map(AssetPairRecord::quote)
                .distinct()
                .sortedWith(assetComparator)

        if (quotes.isEmpty()) {
            pairs_tabs.visibility = View.GONE
        } else {
            pairs_tabs.visibility = View.VISIBLE
        }

        pairs_tabs.setItems(quotes.map { PickerItem(it.code, it.code) })
        displayPairs()
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            assetPairsRepository.updateIfNotFresh()
        } else {
            assetPairsRepository.update()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateListColumnsCount()
    }

    private fun updateListColumnsCount() {
        layoutManager.spanCount = ColumnCalculator.getColumnCount(requireActivity())
        pairsAdapter.drawDividers = layoutManager.spanCount == 1
    }

    override fun onBackPressed(): Boolean {
        return searchItem?.isActionViewExpanded == false.also {
            searchItem?.collapseActionView()
        }
    }

    companion object {
        val ID = "asset_pairs_fragment".hashCode().toLong()

        fun newInstance() = TradeAssetPairsFragment()
    }
}