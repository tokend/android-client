package org.tokend.template.base.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_offers.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.sdk.api.base.model.transactions.InvestmentTransaction
import org.tokend.sdk.api.base.model.transactions.MatchTransaction
import org.tokend.sdk.api.base.model.transactions.Transaction
import org.tokend.template.R
import org.tokend.template.base.activities.tx_details.OfferMatchDetailsActivity
import org.tokend.template.base.activities.tx_details.TxDetailsActivity
import org.tokend.template.base.view.adapter.history.TxHistoryAdapter
import org.tokend.template.base.view.adapter.history.TxHistoryItem
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.features.invest.activities.InvestmentDetailsActivity
import org.tokend.template.features.trade.repository.offers.OffersRepository
import org.tokend.template.util.ObservableTransformers

class OffersActivity : BaseActivity() {
    private val onlyPrimary: Boolean
        get() = intent.getBooleanExtra(ONLY_PRIMARY_EXTRA, false)

    private val offersRepository: OffersRepository
        get() = repositoryProvider.offers(onlyPrimary)

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val txAdapter = TxHistoryAdapter()

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_offers)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (onlyPrimary) {
            setTitle(R.string.pending_investments_title)
        } else {
            setTitle(R.string.pending_offers_title)
        }

        initSwipeRefresh()
        initHistory()

        subscribeToOffers()

        update()
    }

    // region Init
    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }

    private fun initHistory() {
        txAdapter.onItemClick { _, item ->
            openDetails(item.source)
        }

        error_empty_view.observeAdapter(txAdapter) {
            if (onlyPrimary)
                getString(R.string.no_pending_investments)
            else
                getString(R.string.no_pending_offers)
        }
        error_empty_view.setEmptyViewDenial { offersRepository.isNeverUpdated }

        history_list.adapter = txAdapter
        history_list.layoutManager = LinearLayoutManager(this)

        history_list.listenBottomReach({ txAdapter.getDataItemCount() }) {
            offersRepository.loadMore() || offersRepository.noMoreItems
        }

        date_text_switcher.init(history_list, txAdapter)
    }
    // endregion

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
                            txAdapter.showLoadingFooter()
                        }
                    } else {
                        loadingIndicator.hide("offers")
                        txAdapter.hideLoadingFooter()
                    }
                }
                .addTo(compositeDisposable)

        offersRepository.errorsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { error ->
                    if (!txAdapter.hasData) {
                        error_empty_view.showError(error, errorHandlerFactory.getDefault()) {
                            update(true)
                        }
                    } else {
                        errorHandlerFactory.getDefault().handle(error)
                    }
                }
                .addTo(compositeDisposable)
    }

    private fun displayOffers(items: List<Offer>) {
        txAdapter.setData(
                items

                        .map {
                            if (onlyPrimary)
                                InvestmentTransaction.fromOffer(it)
                            else
                                MatchTransaction.fromOffer(it)
                        }
                        .map {
                            TxHistoryItem.fromTransaction(it)
                        }
        )
    }

    private fun openDetails(tx: Transaction?) {
        when (tx) {
            is InvestmentTransaction -> TxDetailsActivity
                    .startForResult<InvestmentDetailsActivity, InvestmentTransaction>(
                            this, tx, CANCEL_OFFER_REQUEST
                    )
            is MatchTransaction -> TxDetailsActivity
                    .startForResult<OfferMatchDetailsActivity, MatchTransaction>(
                            this, tx, CANCEL_OFFER_REQUEST)
        }
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            offersRepository.updateIfNotFresh()
        } else {
            offersRepository.update()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setResult(resultCode)
    }

    companion object {
        const val ONLY_PRIMARY_EXTRA = "only_primary"
        private val CANCEL_OFFER_REQUEST = "cancel_offer".hashCode() and 0xffff
    }
}
