package org.tokend.template.features.trade.history.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_trade_history.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.tokend.template.R
import org.tokend.template.extensions.withArguments
import org.tokend.template.features.trade.history.repository.TradeHistoryRepository
import org.tokend.template.features.trade.history.view.adapter.TradeHistoryAdapter
import org.tokend.template.features.trade.pairs.model.AssetPairRecord
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.formatter.DateFormatters

class TradeHistoryFragment : BaseFragment() {

    private lateinit var assetPair: AssetPairRecord


    private val tradeHistoryRepository: TradeHistoryRepository
        get() = repositoryProvider.tradeHistory(assetPair.base.code, assetPair.quote.code)

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private lateinit var adapter: TradeHistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trade_history, container, false)
    }

    override fun onInitAllowed() {
        assetPair = arguments?.getSerializable(EXTRA_ASSET_PAIR) as? AssetPairRecord
                ?: return

        initViews()
        subscribeToTradeHistory()
        update()
    }

    private fun initViews() {
        initSwipeRefresh()
        initFields()
        initList()
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(true) }
    }

    private fun initFields() {
        price_hint.text = getString(R.string.template_price_hint, assetPair.quote)
        amount_hint.text = getString(R.string.template_amount_hint, assetPair.base)
    }

    private fun initList() {
        trade_history_list.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        adapter = TradeHistoryAdapter(amountFormatter, DateFormatters.timeOrDate(requireContext()))
        trade_history_list.adapter = adapter
        trade_history_list.listenBottomReach({ adapter.getDataItemCount() }) {
            tradeHistoryRepository.loadMore() || tradeHistoryRepository.noMoreItems
        }

        error_empty_view.observeAdapter(adapter, R.string.no_trades_history)
        error_empty_view.setEmptyViewDenial { tradeHistoryRepository.isNeverUpdated }
    }

    private fun subscribeToTradeHistory() {
        tradeHistoryRepository
                .itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    adapter.setData(it)
                }
                .addTo(compositeDisposable)

        tradeHistoryRepository
                .loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { loading ->
                    if (loading) {
                        if (tradeHistoryRepository.isOnFirstPage) {
                            loadingIndicator.setLoading(true)
                        } else {
                            adapter.showLoadingFooter()
                        }
                    } else {
                        loadingIndicator.setLoading(false)
                        adapter.hideLoadingFooter()
                    }
                }
                .addTo(compositeDisposable)

        tradeHistoryRepository
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

    private fun update(force: Boolean = false) {
        if (force) {
            tradeHistoryRepository.update()
        } else {
            tradeHistoryRepository.updateIfNotFresh()
        }
    }

    companion object {
        private const val EXTRA_ASSET_PAIR = "asset_pair"

        fun newInstance(bundle: Bundle): TradeHistoryFragment =
                TradeHistoryFragment().withArguments(bundle)

        fun getBundle(assetPair: AssetPairRecord) = Bundle().apply {
            putSerializable(EXTRA_ASSET_PAIR, assetPair)
        }
    }
}