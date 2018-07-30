package org.tokend.template.features.dashboard


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.models.transactions.MatchTransaction
import org.tokend.template.R
import org.tokend.template.base.fragments.BaseFragment
import org.tokend.template.base.fragments.ToolbarProvider
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.template.base.logic.repository.base.MultipleItemsRepository
import org.tokend.template.base.logic.repository.transactions.TxRepository
import org.tokend.template.base.view.adapter.history.TxHistoryAdapter
import org.tokend.template.base.view.adapter.history.TxHistoryItem
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.features.trade.repository.offers.OffersRepository
import org.tokend.template.util.Navigator
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

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()
    private val txRepository: TxRepository
        get() = repositoryProvider.transactions(asset)
    private val offersRepository: OffersRepository
        get() = repositoryProvider.offers()

    private val activityAdapter = TxHistoryAdapter(true)
    private val offersAdapter = TxHistoryAdapter(true)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)

        toolbar.title = getString(R.string.dashboard_title)

        initAssetTabs()
        initBalance()
        initRecentActivity()
        initPendingOffers()

        subscribeToBalances()
        subscribeToOffers()

        update()
    }

    // region Init
    private fun initAssetTabs() {
        asset_tabs.onItemSelected {
            asset = it.text
        }
    }

    private fun initBalance() {
        converted_balance_text_view.text = "0 USD"
    }

    private fun getEmptyViewObserver(emptyView: TextView,
                                     emptyText: String,
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
                        emptyView.text = getString(R.string.loading_data)
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
        activityAdapter.registerAdapterDataObserver(
                getEmptyViewObserver(empty_view,
                        getString(R.string.no_transaction_history),
                        activity_layout) {
                    txRepository
                }
        )

        activity_list.layoutManager = LinearLayoutManager(context)
        activity_list.adapter = activityAdapter
        activity_list.isNestedScrollingEnabled = false

        view_more_button.onClick {
            Navigator.openWallet(this, SEND_REQUEST, asset)
        }
    }

    private fun initPendingOffers() {
        offersAdapter.registerAdapterDataObserver(
                getEmptyViewObserver(offers_empty_view,
                        getString(R.string.no_pending_offers),
                        offers_layout) {
                    offersRepository
                }
        )

        offers_list.layoutManager = LinearLayoutManager(context)
        offers_list.adapter = offersAdapter
        offers_list.isNestedScrollingEnabled = false

        view_more_offers_button.onClick {
            Navigator.openPendingOffers(this, CANCEL_OFFER_REQUEST)
        }
    }
    // endregion

    // region Subscriptions
    private fun subscribeToBalances() {
        balancesRepository.itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    displayAssetTabs(it.map { it.asset })
                    displayBalance()
                }
                .addTo(compositeDisposable)
        balancesRepository.loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    loadingIndicator.setLoading(it, "balances")
                }
                .addTo(compositeDisposable)
        balancesRepository.errorsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    ErrorHandlerFactory.getDefault().handle(it)
                }
                .addTo(compositeDisposable)
    }

    private var transactionsDisposable: Disposable? = null
    private var transactionsLoadingDisposable: Disposable? = null
    private fun subscribeToTransactions() {
        transactionsDisposable?.dispose()
        transactionsDisposable =
                txRepository.itemsSubject
                        .map { it.subList(0, Math.min(it.size, TRANSACTIONS_TO_DISPLAY)) }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            activityAdapter.setData(it.map {
                                TxHistoryItem.fromTransaction(it)
                            })
                        }
                        .addTo(compositeDisposable)

        transactionsLoadingDisposable?.dispose()
        transactionsLoadingDisposable =
                txRepository.loadingSubject
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { loading ->
                            if (loading) {
                                loadingIndicator.show("transactions")
                                empty_view.text = getString(R.string.loading_data)
                            } else {
                                loadingIndicator.hide("transactions")
                            }
                        }
                        .addTo(compositeDisposable)
    }

    private var offersDisposable: CompositeDisposable? = null

    private fun subscribeToOffers() {
        offersDisposable?.dispose()
        offersDisposable = CompositeDisposable(
                offersRepository.itemsSubject
                        .map { it.subList(0, Math.min(it.size, TRANSACTIONS_TO_DISPLAY)) }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            offersAdapter.setData(it.map {
                                TxHistoryItem.fromTransaction(
                                        MatchTransaction.fromOffer(it)
                                )
                            })
                        },
                offersRepository.loadingSubject
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { loading ->
                            if (loading) {
                                offers_progress.show()
                                offers_empty_view.text = getString(R.string.loading_data)
                            } else {
                                offers_progress.hide()
                            }
                        }
        ).also { it.addTo(compositeDisposable) }
    }
    // endregion

    // region Display
    private fun displayAssetTabs(assets: List<String>) {
        asset_tabs.setSimpleItems(assets)
    }

    private fun displayBalance() {
        balancesRepository.itemsSubject.value
                .find { it.asset == asset }
                ?.let { balanceItem ->
                    val balance = balanceItem.balance
                    balance_text_view.text = AmountFormatter.formatAssetAmount(balance, asset) +
                            " $asset"
                    val converted = balanceItem.convertedBalance
                    val conversionAsset = balanceItem.conversionAsset
                    converted_balance_text_view.text =
                            AmountFormatter.formatAssetAmount(converted, conversionAsset) +
                            " $conversionAsset"
                }
    }
    // endregion

    private fun update() {
        balancesRepository.updateIfNotFresh()
        offersRepository.updateIfNotFresh()
    }

    private fun onAssetChanged() {
        displayBalance()
        subscribeToTransactions()
        txRepository.updateIfNotFresh()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CANCEL_OFFER_REQUEST,
                SEND_REQUEST -> update()
            }
        }
    }
    companion object {
        private val CANCEL_OFFER_REQUEST = "cancel_offer".hashCode() and 0xffff
        private val SEND_REQUEST = "send".hashCode() and 0xffff
        private const val TRANSACTIONS_TO_DISPLAY = 3
        const val ID = 1110L

        fun newInstance(): DashboardFragment {
            val fragment = DashboardFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}
