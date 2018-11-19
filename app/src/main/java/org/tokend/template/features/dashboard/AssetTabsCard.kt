package org.tokend.template.features.dashboard

import android.app.Activity
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.layout_asset_tabs_card.view.*
import kotlinx.android.synthetic.main.layout_progress.view.*
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.data.repository.base.MultipleItemsRepository
import org.tokend.template.data.repository.transactions.TxRepository
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.errorhandler.ErrorHandlerFactory
import org.tokend.template.view.adapter.history.TxHistoryAdapter
import org.tokend.template.view.adapter.history.TxHistoryItem
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.ViewProvider
import org.tokend.template.view.util.formatter.AmountFormatter

class AssetTabsCard(private val activity: Activity,
                    private val repositoryProvider: RepositoryProvider,
                    private val errorHandlerFactory: ErrorHandlerFactory,
                    private val disposable: CompositeDisposable) : ViewProvider {

    private lateinit var view: View

    private lateinit var loadingIndicator: LoadingIndicatorManager

    private var asset: String = ""
        set(value) {
            field = value
            onAssetChanged()
        }

    private val activityAdapter = TxHistoryAdapter(true)

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private val txRepository: TxRepository
        get() = repositoryProvider.transactions(asset)

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

        subscribeToBalances()
        balancesRepository.updateIfNotFresh()

        return view
    }

    fun initViewMoreButton(fragment: Fragment) {
        view.view_more_button.onClick {
            Navigator.openWallet(fragment, SEND_REQUEST, asset)
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

    private fun getEmptyViewObserver(emptyView: TextView,
                                     emptyText: String?,
                                     contentView: View,
                                     repository: () -> MultipleItemsRepository<out Any>
    ): RecyclerView.AdapterDataObserver {
        return object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                if (repository().itemsSubject.value.isNotEmpty()) {
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
        activityAdapter.onItemClick { _, item ->
            item.source?.let { Navigator.openTransactionDetails(activity, it) }
        }

        activityAdapter.registerAdapterDataObserver(
                getEmptyViewObserver(view.empty_view,
                        activity.getString(R.string.no_transaction_history),
                        view.activity_layout) {
                    txRepository
                }
        )

        view.activity_list.layoutManager = LinearLayoutManager(activity)
        view.activity_list.adapter = activityAdapter
        view.activity_list.isNestedScrollingEnabled = false
    }

    private fun onAssetChanged() {
        displayBalance()
        subscribeToTransactions()
        txRepository.updateIfNotFresh()
    }

    private fun displayAssetTabs(assets: List<String>) {
        view.asset_tabs.setSimpleItems(assets)
    }

    private fun displayBalance() {
        balancesRepository.itemsSubject.value
                .find { it.asset == asset }
                ?.let { balanceItem ->
                    val balance = balanceItem.balance
                    view.balance_text_view.text =
                            AmountFormatter.formatAssetAmount(balance, asset) + " $asset"
                    val converted = balanceItem.convertedBalance
                    val conversionAsset = balanceItem.conversionAsset
                    view.converted_balance_text_view.text =
                            AmountFormatter.formatAssetAmount(converted, conversionAsset) +
                            " $conversionAsset"
                }
    }

    private var balancesDisposable: CompositeDisposable? = null
    private fun subscribeToBalances() {
        balancesDisposable?.dispose()
        balancesDisposable = CompositeDisposable(
                balancesRepository.itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    displayAssetTabs(it.map { it.asset })
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

    private var transactionsDisposable: CompositeDisposable? = null
    private fun subscribeToTransactions() {
        transactionsDisposable?.dispose()
        transactionsDisposable = CompositeDisposable(
                txRepository.itemsSubject
                .map { it.subList(0, Math.min(it.size, TRANSACTIONS_TO_DISPLAY)) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    activityAdapter.setData(it.map {
                        TxHistoryItem.fromTransaction(it)
                    })
                },
            txRepository.loadingSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { loading ->
                    if (loading) {
                        loadingIndicator.show("transactions")
                        view.empty_view.text = activity.getString(R.string.loading_data)
                    } else {
                        loadingIndicator.hide("transactions")
                    }
                }
        ).also { it.addTo(disposable) }
    }

    companion object {
        private const val TRANSACTIONS_TO_DISPLAY = 3
        val SEND_REQUEST = "send".hashCode() and 0xffff
    }
}