package io.tokend.template.features.dashboard.movements.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.rxkotlin.addTo
import io.tokend.template.R
import io.tokend.template.features.history.storage.BalanceChangesRepository
import io.tokend.template.features.history.view.adapter.BalanceChangeListItem
import io.tokend.template.features.history.view.adapter.BalanceChangesAdapter
import io.tokend.template.fragments.BaseFragment
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.util.navigation.Navigator
import io.tokend.template.view.util.ElevationUtil
import io.tokend.template.view.util.LoadingIndicatorManager
import io.tokend.template.view.util.LocalizedName
import io.tokend.template.view.util.formatter.DateFormatters
import kotlinx.android.synthetic.main.fragment_account_movements.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.include_error_empty_view.*

class AccountMovementsFragment : BaseFragment() {
    private val loadingIndicator = LoadingIndicatorManager(
        showLoading = { swipe_refresh.isRefreshing = true },
        hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val balanceChangesRepository: BalanceChangesRepository
        get() = repositoryProvider.balanceChanges(null)

    private lateinit var adapter: BalanceChangesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account_movements, container, false)
    }

    override fun onInitAllowed() {
        initSwipeRefresh()
        initHistory()
        ElevationUtil.initScrollElevation(history_list, appbar_elevation_view)

        subscribeToHistory()

        update()
    }

    // region Init
    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }

    private fun initHistory() {
        adapter =
            BalanceChangesAdapter(amountFormatter, DateFormatters.timeOrDate(requireContext()))
        adapter.onItemClick { _, item ->
            item.source?.let { Navigator.from(this).openBalanceChangeDetails(it) }
        }

        error_empty_view.setEmptyDrawable(R.drawable.ic_balance)
        error_empty_view.setPadding(
            0, 0, 0,
            resources.getDimensionPixelSize(R.dimen.quadra_margin)
        )
        error_empty_view.observeAdapter(adapter, R.string.no_transaction_history)
        error_empty_view.setEmptyViewDenial { balanceChangesRepository.isNeverUpdated }

        history_list.adapter = adapter
        history_list.layoutManager = LinearLayoutManager(context!!)
        history_list.setHasFixedSize(true)

        history_list.listenBottomReach({ adapter.getDataItemCount() }) {
            balanceChangesRepository.loadMore() || balanceChangesRepository.noMoreItems
        }

        date_text_switcher.init(history_list, adapter)
    }
    // endregion

    private fun subscribeToHistory() {
        balanceChangesRepository
            .itemsSubject
            .compose(ObservableTransformers.defaultSchedulers())
            .subscribe { displayHistory() }
            .addTo(compositeDisposable)

        balanceChangesRepository
            .loadingSubject
            .compose(ObservableTransformers.defaultSchedulers())
            .subscribe { loading ->
                if (loading) {
                    if (!balanceChangesRepository.isOnFirstPage) {
                        adapter.showLoadingFooter()
                    }

                    if (balanceChangesRepository.isOnFirstPage
                        || balanceChangesRepository.isLoadingTopPages
                    ) {
                        loadingIndicator.show("transactions")
                    }
                } else {
                    loadingIndicator.hide("transactions")
                    adapter.hideLoadingFooter()
                }
            }
            .addTo(compositeDisposable)

        balanceChangesRepository
            .errorsSubject
            .compose(ObservableTransformers.defaultSchedulers())
            .subscribe { error ->
                if (!adapter.hasData) {
                    error_empty_view.showError(error, errorHandlerFactory.getDefault()) {
                        update(true)
                    }
                } else {
                    errorHandlerFactory.getDefault().handle(error)
                }
            }
            .addTo(compositeDisposable)
    }

    private fun displayHistory() {
        val localizedName = LocalizedName(requireContext())
        val accountId = walletInfoProvider.getWalletInfo().accountId

        adapter.setData(balanceChangesRepository.itemsList.map { balanceChange ->
            BalanceChangeListItem(balanceChange, accountId, localizedName)
        })

        history_list.resetBottomReachHandled()
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            balanceChangesRepository.updateIfNotFresh()
        } else {
            balanceChangesRepository.update()
        }
    }

    companion object {
        fun newInstance() = AccountMovementsFragment()
    }
}