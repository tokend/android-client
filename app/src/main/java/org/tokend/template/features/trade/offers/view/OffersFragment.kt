package org.tokend.template.features.trade.offers.view

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_trade_offers.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.tokend.template.R
import org.tokend.template.data.model.AssetPairRecord
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.data.repository.offers.OffersRepository
import org.tokend.template.features.offers.view.PendingOfferListItem
import org.tokend.template.features.offers.view.PendingOffersAdapter
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager

class OffersFragment : BaseFragment() {

    private val assetPair: AssetPairRecord? by lazy {
        arguments?.getSerializable(ASSET_PAIR_EXTRA) as? AssetPairRecord
    }

    private val onlyPrimary: Boolean
        get() = arguments?.getBoolean(ONLY_PRIMARY_EXTRA)
                ?: false

    private val offersRepository: OffersRepository
        get() = repositoryProvider.offers(onlyPrimary)

    private lateinit var adapter: PendingOffersAdapter

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trade_offers, container, false)
    }

    override fun onInitAllowed() {
        initSwipeRefresh()
        initList()
        subscribeToOffers()
        update()
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(true) }
    }

    private fun initList() {
        adapter = PendingOffersAdapter(amountFormatter, false)
        adapter.onItemClick { _, item ->
            item.source?.also {
                Navigator.openPendingOfferDetails(requireActivity(), it)
            }
        }

        error_empty_view.setEmptyDrawable(R.drawable.ic_pending)
        error_empty_view.observeAdapter(adapter) {
            if (onlyPrimary)
                getString(R.string.no_pending_investments)
            else
                getString(R.string.no_pending_offers)
        }
        error_empty_view.setEmptyViewDenial { offersRepository.isNeverUpdated }

        history_list.adapter = adapter
        history_list.layoutManager = LinearLayoutManager(context)

        history_list.listenBottomReach({ adapter.getDataItemCount() }) {
            offersRepository.loadMore() || offersRepository.noMoreItems
        }

        date_text_switcher.init(history_list, adapter)
    }

    private fun subscribeToOffers() {
        offersRepository.itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { displayOffers(it) }
                .addTo(compositeDisposable)

        offersRepository.loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { isLoading ->
                    if (isLoading) {
                        if (offersRepository.isOnFirstPage) {
                            loadingIndicator.show("offers")
                        } else {
                            adapter.showLoadingFooter()
                        }
                    } else {
                        loadingIndicator.hide("offers")
                        adapter.hideLoadingFooter()
                    }
                }
                .addTo(compositeDisposable)

        offersRepository.errorsSubject
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

    private fun displayOffers(items: List<OfferRecord>) {
        adapter.setData(
                items
                        .filter { item ->
                            assetPair?.let {
                                item.baseAssetCode == it.base
                                        && item.quoteAssetCode == it.quote
                            } ?: true
                        }
                        .map {
                            PendingOfferListItem(it)
                        }
        )
    }

    private fun update(force: Boolean = false) {
        if (force) {
            offersRepository.update()
        } else {
            offersRepository.updateIfNotFresh()
        }
    }

    companion object {
        private const val ASSET_PAIR_EXTRA = "asset_pair"
        const val ONLY_PRIMARY_EXTRA = "only_primary"

        fun newInstance(pair: AssetPairRecord): OffersFragment {
            val fragment = OffersFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(ASSET_PAIR_EXTRA, pair)
            }
            return fragment
        }

        fun newInstance(onlyPrimary: Boolean): OffersFragment {
            val fragment = OffersFragment()
            fragment.arguments = Bundle().apply {
                putBoolean(ONLY_PRIMARY_EXTRA, onlyPrimary)
            }
            return fragment
        }
    }
}