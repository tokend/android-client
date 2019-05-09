package org.tokend.template.features.dashboard.balances.view

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_balances.*
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.features.dashboard.balances.view.adapter.BalanceItemsAdapter
import org.tokend.template.features.dashboard.balances.view.adapter.BalanceListItem
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.ScrollOnTopItemUpdateAdapterObserver
import java.math.BigDecimal


class BalancesFragment : BaseFragment() {
    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private lateinit var adapter: BalanceItemsAdapter

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

        subscribeToBalances()

        update()
    }

    // region Init
    private fun initList() {
        adapter = BalanceItemsAdapter(amountFormatter)
        balances_list.adapter = adapter
        balances_list.layoutManager = LinearLayoutManager(requireContext())
        adapter.registerAdapterDataObserver(ScrollOnTopItemUpdateAdapterObserver(balances_list))
        adapter.onItemClick { _, item ->
            item.source?.assetCode?.also { openWallet(it) }
        }
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
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
        displayBalances()
        displayDistribution()
        displayTotal()
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
        val conversionAssetCode = balancesRepository.conversionAssetCode

        if (conversionAssetCode != null) {
            distribution_chart.setData(balancesRepository.itemsList, conversionAssetCode)
        }

        distribution_chart.apply {
            setData(balancesRepository.itemsList, "USD")
            visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun displayTotal() {
        val total = balancesRepository
                .itemsList
                .fold(BigDecimal.ZERO) { sum, balance ->
                    sum.add(balance.convertedAmount ?: BigDecimal.ZERO)
                }
        val conversionAssetCode = balancesRepository.conversionAssetCode
                ?: return

        total_text_view.text = amountFormatter.formatAssetAmount(total, conversionAssetCode)
    }
    // endregion

    private fun openWallet(assetCode: String) {
        Navigator.from(this).openWallet(0, assetCode)
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

    private fun openAssetsExplorer() {
        Navigator.from(this).openAssetsExplorer()
    }
}