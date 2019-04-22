package org.tokend.template.features.dashboard

import android.app.Activity
import android.support.v4.app.Fragment
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.layout_asset_tabs_card.view.*
import kotlinx.android.synthetic.main.layout_progress.view.*
import org.jetbrains.anko.onClick
import org.jetbrains.anko.onTouch
import org.tokend.template.R
import org.tokend.template.data.repository.balancechanges.BalanceChangesRepository
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.data.repository.base.MultipleItemsRepository
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.features.wallet.adapter.BalanceChangeListItem
import org.tokend.template.features.wallet.adapter.BalanceChangesAdapter
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.errorhandler.ErrorHandlerFactory
import org.tokend.template.view.util.HorizontalSwipesGestureDetector
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.LocalizedName
import org.tokend.template.view.util.ViewProvider
import org.tokend.template.view.util.formatter.AmountFormatter
import java.lang.ref.WeakReference
import java.util.*

class AssetTabsCard(private val activity: Activity,
                    private val repositoryProvider: RepositoryProvider,
                    private val errorHandlerFactory: ErrorHandlerFactory,
                    private val assetComparator: Comparator<String>,
                    private val disposable: CompositeDisposable,
                    private val amountFormatter: AmountFormatter,
                    private val accountId: String) : ViewProvider {

    private lateinit var view: View

    private lateinit var loadingIndicator: LoadingIndicatorManager

    private var asset: String = ""
        set(value) {
            field = value
            onAssetChanged()
        }

    private val balanceId: String
        get() = balancesRepository.itemsList.find { it.assetCode == asset }?.id ?: ""

    private lateinit var activityAdapter: BalanceChangesAdapter

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private val historyRepository: BalanceChangesRepository
        get() = repositoryProvider.balanceChanges(balanceId)

    override fun addTo(rootView: ViewGroup): AssetTabsCard {
        rootView.addView(getView(rootView))

        return this
    }

    override fun getView(rootView: ViewGroup): View {
        view = LayoutInflater.from(activity)
                .inflate(R.layout.layout_asset_tabs_card, rootView, false)

        initLoadingManager()
        initAssetTabs()
        initRecentActivity()
        initHorizontalSwipes()

        subscribeToBalances()
        balancesRepository.updateIfNotFresh()

        return view
    }

    fun initViewMoreButton(fragment: Fragment) {
        view.view_more_button.onClick {
            Navigator(fragment).openWallet(SEND_REQUEST, asset)
        }
    }

    private fun initLoadingManager() {
        loadingIndicator = LoadingIndicatorManager(
                showLoading = { view.progress.show() },
                hideLoading = { view.progress.hide() }
        )
    }

    private fun initAssetTabs() {
        view.asset_tabs.onItemSelected {
            asset = it.text
        }
    }

    private fun initHorizontalSwipes() {
        val weakTabs = WeakReference(view.asset_tabs)

        val gestureDetector = GestureDetectorCompat(view.context, HorizontalSwipesGestureDetector(
                onSwipeToLeft = {
                    weakTabs.get()?.apply { selectedItemIndex++ }
                },
                onSwipeToRight = {
                    weakTabs.get()?.apply { selectedItemIndex-- }
                }
        ))

        val weakReceiverLayout = WeakReference(view.content_layout)
        view.touch_capture_layout.onTouch { _, it ->
            if (!gestureDetector.onTouchEvent(it)) {
                weakReceiverLayout.get()?.dispatchTouchEvent(it)
            }
            true
        }
    }

    private fun getEmptyViewObserver(emptyView: TextView,
                                     emptyText: String?,
                                     contentView: View,
                                     repository: () -> MultipleItemsRepository<out Any>
    ): RecyclerView.AdapterDataObserver {
        return object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                if (repository().itemsList.isNotEmpty()) {
                    emptyView.visibility = View.GONE
                    contentView.visibility = View.VISIBLE
                } else {
                    if (repository().isNeverUpdated) {
                        emptyView.text = activity.getString(R.string.loading_data)
                    } else {
                        emptyView.text = emptyText
                    }
                    emptyView.visibility = View.VISIBLE
                    contentView.visibility = View.GONE
                }
            }
        }
    }

    private fun initRecentActivity() {
        activityAdapter = BalanceChangesAdapter(amountFormatter, true)

        activityAdapter.onItemClick { _, item ->
            item.source?.let { Navigator(activity).openBalanceChangeDetails(it) }
        }

        activityAdapter.registerAdapterDataObserver(
                getEmptyViewObserver(view.empty_view,
                        activity.getString(R.string.no_transaction_history),
                        view.activity_layout) {
                    historyRepository
                }
        )

        view.activity_list.layoutManager = LinearLayoutManager(activity)
        view.activity_list.adapter = activityAdapter
        view.activity_list.isNestedScrollingEnabled = false
    }

    private fun onAssetChanged() {
        displayBalance()
        subscribeToTransactions()
        historyRepository.updateIfNotFresh()
    }

    private fun displayAssetTabs(assets: List<String>) {
        Collections.sort(assets, assetComparator)
        view.asset_tabs.setSimpleItems(assets)
    }

    private fun displayBalance() {
        balancesRepository.itemsList
                .find { it.assetCode == asset }
                ?.let { balance ->
                    view.balance_text_view.text =
                            amountFormatter.formatAssetAmount(balance.available, asset)
                }
    }

    private var balancesDisposable: CompositeDisposable? = null
    private fun subscribeToBalances() {
        balancesDisposable?.dispose()
        balancesDisposable = CompositeDisposable(
                balancesRepository.itemsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { balances ->
                            displayAssetTabs(balances.map { it.assetCode })
                            displayBalance()
                        },
                balancesRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            loadingIndicator.setLoading(it, "balances")
                        },
                balancesRepository.errorsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            errorHandlerFactory.getDefault().handle(it)
                        }
        ).also { it.addTo(disposable) }
    }

    private var transactionsDisposable: Disposable? = null
    private fun subscribeToTransactions() {
        transactionsDisposable?.dispose()
        transactionsDisposable = CompositeDisposable(
                historyRepository.itemsSubject
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { displayHistory() },
                historyRepository.loadingSubject
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { loading ->
                            if (loading) {
                                loadingIndicator.show("transactions")
                                view.empty_view.text = activity.getString(R.string.loading_data)
                            } else {
                                loadingIndicator.hide("transactions")
                            }
                        }
        ).addTo(disposable)
    }

    private fun displayHistory() {
        val localizedName = LocalizedName(activity)

        val items = historyRepository
                .itemsList
                .let { it.subList(0, Math.min(it.size, TRANSACTIONS_TO_DISPLAY)) }
                .map { balanceChange ->
                    BalanceChangeListItem(balanceChange, accountId, localizedName)
                }

        activityAdapter.setData(items)
    }

    companion object {
        private const val TRANSACTIONS_TO_DISPLAY = 3
        val SEND_REQUEST = "send".hashCode() and 0xffff
    }
}