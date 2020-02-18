package org.tokend.template.features.dashboard.balances.view

import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.view.*
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_balances.*
import kotlinx.android.synthetic.main.fragment_balances_collapsing_content.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.features.balances.storage.BalancesRepository
import org.tokend.template.features.dashboard.balances.view.adapter.BalanceItemsAdapter
import org.tokend.template.features.dashboard.balances.view.adapter.BalanceListItem
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.navigation.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.*
import java.math.BigDecimal


class BalancesFragment : BaseFragment() {
    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private lateinit var adapter: BalanceItemsAdapter
    private lateinit var layoutManager: GridLayoutManager

    private var chartEverAnimated = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balances, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onInitAllowed() {
        initList()
        initSwipeRefresh()
        initToolbarElevation()

        subscribeToBalances()

        update()
    }

    // region Init
    private fun initList() {
        layoutManager = GridLayoutManager(requireContext(), 1)
        adapter = BalanceItemsAdapter(amountFormatter)
        updateListColumnsCount()
        balances_list.adapter = adapter
        balances_list.layoutManager = layoutManager
        adapter.registerAdapterDataObserver(ScrollOnTopItemUpdateAdapterObserver(balances_list))
        adapter.onItemClick { _, item ->
            item.source?.id?.also { openWallet(it) }
        }

        error_empty_view.observeAdapter(adapter, R.string.you_have_no_balances)
        error_empty_view.setEmptyViewDenial { balancesRepository.isNeverUpdated }
        error_empty_view.setEmptyDrawable(R.drawable.ic_coins)
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
        SwipeRefreshDependencyUtil.addDependency(swipe_refresh, app_bar)
    }

    private fun initToolbarElevation() {
        ElevationUtil.initScrollElevation(app_bar, appbar_elevation_view)
    }
    // endregion

    private fun update(force: Boolean = false) {
        if (!force) {
            balancesRepository.updateIfNotFresh()
        } else {
            balancesRepository.update()
        }
    }

    private fun subscribeToBalances() {
        balancesRepository
                .itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { onBalancesUpdated() }
                .addTo(compositeDisposable)

        balancesRepository
                .loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { loadingIndicator.setLoading(it) }
                .addTo(compositeDisposable)
    }

    private fun onBalancesUpdated() {
        initCollapsingContentOnce()
        displayBalances()
        displayDistribution()
        displayTotal()
    }

    private var collapsingContentInitCompleted = false
    private fun initCollapsingContentOnce() {
        if (collapsingContentInitCompleted) {
            return
        }

        collapsing_content_stub.inflate()

        collapsingContentInitCompleted = true
    }

    // region Display
    private fun displayBalances() {
        val items = balancesRepository
                .itemsList
                .sortedWith(balanceComparator)
                .map(::BalanceListItem)

        adapter.setData(items)
    }

    private fun displayDistribution() {
        val conversionAsset = balancesRepository.conversionAsset

        if (conversionAsset == null) {
            distribution_chart.visibility = View.GONE
            return
        }

        distribution_chart.apply {
            setData(
                    balancesRepository.itemsList,
                    conversionAsset,
                    !chartEverAnimated
            )
            chartEverAnimated = true
            visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun displayTotal() {
        val conversionAssetCode = balancesRepository.conversionAsset

        if (conversionAssetCode == null) {
            total_text_view.visibility = View.GONE
            return
        }

        val total = balancesRepository
                .itemsList
                .fold(BigDecimal.ZERO) { sum, balance ->
                    sum.add(balance.convertedAmount ?: BigDecimal.ZERO)
                }

        total_text_view.visibility = View.VISIBLE
        total_text_view.text = amountFormatter.formatAssetAmount(total, conversionAssetCode)
    }
    // endregion

    private fun openWallet(balanceId: String) {
        Navigator.from(this).openBalanceDetails(balanceId)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.balances, menu)

        val assetsExplorerItem = menu.findItem(R.id.add)

        if (!BuildConfig.IS_EXPLORE_ALLOWED) {
            assetsExplorerItem.isVisible = false
            return
        }

        assetsExplorerItem.setOnMenuItemClickListener {
            openAssetsExplorer()
            true
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        updateListColumnsCount()
    }

    private fun updateListColumnsCount() {
        layoutManager.spanCount = ColumnCalculator.getColumnCount(requireActivity())
        adapter.drawDividers = layoutManager.spanCount == 1
    }

    private fun openAssetsExplorer() {
        Navigator.from(this).openAssetsExplorer()
    }

    companion object {
        fun newInstance() = BalancesFragment()
    }
}