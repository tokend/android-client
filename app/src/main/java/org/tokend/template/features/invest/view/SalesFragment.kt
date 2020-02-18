package org.tokend.template.features.invest.view

import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_sales.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.features.invest.logic.SalesSubscriptionManager
import org.tokend.template.features.invest.repository.SalesRepository
import org.tokend.template.features.invest.view.adapter.SalesAdapter
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.util.navigation.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.*

class SalesFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val salesRepository: SalesRepository
        get() = repositoryProvider.sales()
    private val filterSalesRepository: SalesRepository
        get() = repositoryProvider.filteredSales()

    private lateinit var salesAdapter: SalesAdapter

    private var searchItem: MenuItem? = null
    private var tokenQuery = ""
    private val hasFilter: Boolean
        get() = tokenQuery.isNotEmpty()

    private lateinit var salesSubscriptionManager: SalesSubscriptionManager
    private lateinit var layoutManager: GridLayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sales, container, false)
    }

    override fun onInitAllowed() {
        initToolbar()
        initSwipeRefresh()
        initSalesList()
        initSubscriptionManager()

        update()
    }

    // region Init
    private fun initToolbar() {
        toolbarSubject.onNext(toolbar)
        toolbar.title = getString(R.string.explore_sales_title)
        initMenu()
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }

    private fun initSalesList() {
        val columns = ColumnCalculator.getColumnCount(requireActivity())

        layoutManager = GridLayoutManager(context, columns)

        salesAdapter = SalesAdapter(urlConfigProvider.getConfig().storage, amountFormatter)
        error_empty_view.setEmptyDrawable(R.drawable.ic_invest)
        error_empty_view.observeAdapter(salesAdapter, R.string.no_sales_found)

        salesAdapter.onItemClick { _, sale ->
            Navigator.from(this)
                    .openSale(sale)
                    .addTo(activityRequestsBag)
                    .doOnSuccess { update(force = true) }
        }

        sales_list.apply {
            layoutManager = this@SalesFragment.layoutManager
            adapter = salesAdapter

            setItemViewCacheSize(20)
            (sales_list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }

        salesAdapter.registerAdapterDataObserver(
                ScrollOnTopItemUpdateAdapterObserver(sales_list)
        )

        ElevationUtil.initScrollElevation(sales_list, appbar_elevation_view)
    }

    private fun initSubscriptionManager() {
        salesSubscriptionManager = SalesSubscriptionManager(
                sales_list,
                salesAdapter,
                loadingIndicator,
                error_empty_view,
                compositeDisposable,
                errorHandlerFactory
        ) {
            update()
        }
    }

    private fun initMenu() {
        toolbar.inflateMenu(R.menu.sales)
        val menu = toolbar.menu

        val pendingOffersItem = menu?.findItem(R.id.pending_offers)
        searchItem = menu?.findItem(R.id.search) ?: return

        val updateFilter = { newQuery: String ->
            val prevTokenQuery = tokenQuery
            tokenQuery = newQuery

            if (prevTokenQuery != tokenQuery && tokenQuery.isNotEmpty()) {
                update(true)
            } else {
                update()
            }
        }

        searchItem = menu.findItem(R.id.search) ?: return
        val searchItem = menu.findItem(R.id.search) ?: return

        try {
            val searchManager = MenuSearchViewManager(searchItem, toolbar, compositeDisposable)

            searchManager.queryHint = getString(R.string.sales_search_asset_hint)
            searchManager
                    .queryChanges
                    .compose(ObservableTransformers.defaultSchedulers())
                    .subscribe { newValue ->
                        updateFilter(newValue)
                    }
                    .addTo(compositeDisposable)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        pendingOffersItem?.setOnMenuItemClickListener {
            Navigator.from(this)
                    .openPendingOffers(true)
                    .addTo(activityRequestsBag)
                    .doOnSuccess { update(force = true) }
            true
        }
    }
    // endregion

    private fun update(force: Boolean = false) {
        if (!hasFilter) {
            salesSubscriptionManager
                    .subscribeTo(
                            repository = salesRepository,
                            force = force)
        } else {
            salesSubscriptionManager
                    .subscribeTo(
                            filterSalesRepository,
                            tokenQuery,
                            force
                    )
        }
    }

    override fun onBackPressed(): Boolean {
        return searchItem?.isActionViewExpanded == false.also {
            searchItem?.collapseActionView()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        layoutManager.spanCount = ColumnCalculator.getColumnCount(requireActivity())
    }

    companion object {
        val ID = "sales_fragment".hashCode().toLong()

        fun newInstance() = SalesFragment()
    }
}