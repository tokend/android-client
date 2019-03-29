package org.tokend.template.features.trade.pairs.view

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_trade_asset_pairs.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.data.repository.pairs.AssetPairsRepository
import org.tokend.template.features.trade.pairs.view.adapter.AssetPairItemsAdapter
import org.tokend.template.features.trade.pairs.view.adapter.AssetPairListItem
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.SearchUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.MenuSearchViewManager

class TradeAssetPairsFragment : BaseFragment(), ToolbarProvider {

    private val assetPairsRepository: AssetPairsRepository
        get() = repositoryProvider.assetPairs()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private var searchItem: MenuItem? = null

    private lateinit var pairsAdapter: AssetPairItemsAdapter

    private var filter: String? = null
        set(value) {
            if (value != field) {
                field = value
                onFilterChanged()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trade_asset_pairs, container, false)
    }

    override fun onInitAllowed() {
        initToolbar()
        initSwipeRefresh()
        initList()

        subscribeToAssetPairs()

        update()
    }

    // region Init
    private fun initToolbar() {
        toolbarSubject.onNext(toolbar)
        toolbar.title = context?.getString(R.string.trade_title)

        initMenu()
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
            Navigator.openPendingOffers(this, 0, false)
            true
        }
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.accent))
        swipe_refresh.setOnRefreshListener { update(true) }
    }

    private fun initList() {
        pairsAdapter = AssetPairItemsAdapter(amountFormatter)

        asset_pairs_recycler_view.layoutManager = LinearLayoutManager(requireContext())
        asset_pairs_recycler_view.adapter = pairsAdapter

        error_empty_view.setEmptyDrawable(R.drawable.ic_trade)
        error_empty_view.observeAdapter(pairsAdapter, R.string.error_no_tradeable_pairs)
        error_empty_view.setEmptyViewDenial { assetPairsRepository.isNeverUpdated }

        pairsAdapter.onItemClick { _, item ->
            item.source?.let { assetPair ->
                Navigator.openTrade(this, assetPair)
            }
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
                .subscribe { displayPairs() }
                .addTo(compositeDisposable)
    }

    private fun onFilterChanged() {
        displayPairs()
    }

    private fun displayPairs() {
        val comparator = Comparator<AssetPairListItem> { o1, o2 ->
            assetComparator.compare(o1.baseAssetCode, o2.baseAssetCode)
        }

        val items = assetPairsRepository
                .itemsList
                .map { AssetPairListItem(it, requireContext()) }
                .sortedWith(comparator)
                .let { items ->
                    filter?.let {
                        items.filter { item ->
                            SearchUtil.isMatchGeneralCondition(
                                    it,
                                    item.baseAssetCode,
                                    item.quoteAssetCode,
                                    item.code
                            )
                        }
                    } ?: items
                }

        pairsAdapter.setData(items)
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            assetPairsRepository.updateIfNotFresh()
        } else {
            assetPairsRepository.update()
        }
    }

    override fun onBackPressed(): Boolean {
        return searchItem?.isActionViewExpanded == false.also {
            searchItem?.collapseActionView()
        }
    }

    companion object {
        val ID = "asset_pairs_fragment".hashCode().toLong()
    }
}