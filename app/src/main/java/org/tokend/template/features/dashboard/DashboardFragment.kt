package org.tokend.template.features.dashboard


import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.trello.rxlifecycle2.android.FragmentEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.base.fragments.BaseFragment
import org.tokend.template.base.fragments.ToolbarProvider
import org.tokend.template.base.logic.repository.TxRepository
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.template.base.view.adapter.history.TxHistoryAdapter
import org.tokend.template.base.view.adapter.history.TxHistoryItem
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.error_handlers.ErrorHandlerFactory

class DashboardFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { activity_progress.show() },
            hideLoading = { activity_progress.hide() }
    )

    private var asset: String = ""
        set(value) {
            field = value
            onAssetChanged()
        }

    private lateinit var balancesRepository: BalancesRepository
    private val txRepository: TxRepository
        get() = repositoryProvider.transactions(asset)

    private val activityAdapter = TxHistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        balancesRepository = repositoryProvider.balances()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbarSubject.onNext(toolbar)

        toolbar.title = getString(R.string.dashboard_title)

        initAssetTabs()
        initBalance()
        initRecentActivity()

        subscribeToBalances()

        update()
    }

    // region Init
    private fun initAssetTabs() {
        asset_tabs.onItemSelected {
            asset = it
        }
    }

    private fun initBalance() {
        converted_balance_text_view.text = "0 USD"
    }

    private val emptyViewObserver =
            object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    if (activityAdapter.hasData) {
                        empty_view.visibility = View.GONE
                        activity_layout.visibility = View.VISIBLE
                    } else {
                        if (txRepository.isNeverUpdated) {
                            empty_view.text = getString(R.string.loading_data)
                        } else {
                            empty_view.text = getString(R.string.no_transaction_history)
                        }
                        empty_view.visibility = View.VISIBLE
                        activity_layout.visibility = View.GONE
                    }
                }
            }

    private fun initRecentActivity() {
        activityAdapter.registerAdapterDataObserver(emptyViewObserver)

        activity_list.layoutManager = LinearLayoutManager(context)
        activity_list.adapter = activityAdapter
        activity_list.isNestedScrollingEnabled = false

        view_more_button.onClick {
            it?.isEnabled = false
        }
    }
    // endregion

    private fun subscribeToBalances() {
        balancesRepository.itemsSubject
                .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    displayAssetTabs(it.map { it.asset })
                    displayBalance()
                }
        balancesRepository.loadingSubject
                .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    loadingIndicator.setLoading(it, "balances")
                }
        balancesRepository.errorsSubject
                .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    ErrorHandlerFactory.getDefault().handle(it)
                }
    }

    private var transactionsDisposable: Disposable? = null
    private var transactionsLoadingDisposable: Disposable? = null
    private fun subscribeToTransactions() {
        val contextAccountId = walletInfoProvider.getWalletInfo()?.accountId ?: ""
        transactionsDisposable?.dispose()
        transactionsDisposable =
                txRepository.itemsSubject
                        .map { it.subList(0, Math.min(it.size, TRANSACTIONS_TO_DISPLAY)) }
                        .observeOn(AndroidSchedulers.mainThread())
                        .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                        .subscribe {
                            activityAdapter.setData(it.map {
                                TxHistoryItem.fromTransaction(
                                        contextAccountId,
                                        it
                                )
                            })
                        }

        transactionsLoadingDisposable?.dispose()
        transactionsLoadingDisposable =
                txRepository.loadingSubject
                        .observeOn(AndroidSchedulers.mainThread())
                        .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY_VIEW)
                        .subscribe { loading ->
                            if (loading) {
                                loadingIndicator.show("transactions")
                                empty_view.text = getString(R.string.loading_data)
                            } else {
                                loadingIndicator.hide("transactions")
                            }
                        }
    }

    private fun displayAssetTabs(assets: List<String>) {
        asset_tabs.setItems(assets, true)
    }

    private fun displayBalance() {
        balancesRepository.itemsSubject.value
                .find { it.asset == asset }
                ?.let { balanceItem ->
                    balance_text_view.text = AmountFormatter.formatAssetAmount(
                            balanceItem.balance, balanceItem.asset
                    ) + " $asset"
                }
    }

    private fun update() {
        balancesRepository.updateIfNotFresh()
    }

    private fun onAssetChanged() {
        displayBalance()
        subscribeToTransactions()
        txRepository.updateIfNotFresh()
    }

    companion object {
        private const val TRANSACTIONS_TO_DISPLAY = 3

        fun newInstance(): DashboardFragment {
            val fragment = DashboardFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}
