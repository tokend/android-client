package org.tokend.template.features.trade.adapter

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import kotlinx.android.synthetic.main.activity_offers.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.tokend.sdk.api.models.Offer
import org.tokend.sdk.api.models.transactions.BaseTransaction
import org.tokend.sdk.api.models.transactions.MatchTransaction
import org.tokend.sdk.api.models.transactions.Transaction
import org.tokend.template.R
import org.tokend.template.base.activities.BaseActivity
import org.tokend.template.base.activities.tx_details.OfferMatchDetailsActivity
import org.tokend.template.base.activities.tx_details.TxDetailsActivity
import org.tokend.template.base.activities.tx_details.UnknownTxDetailsActivity
import org.tokend.template.base.view.adapter.history.TxHistoryAdapter
import org.tokend.template.base.view.adapter.history.TxHistoryItem
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.features.trade.repository.offers.OffersRepository
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.error_handlers.ErrorHandlerFactory

class OffersActivity : BaseActivity() {
    private val offersRepository: OffersRepository
        get() = repositoryProvider.offers()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val txAdapter = TxHistoryAdapter()

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_offers)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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

        error_empty_view.observeAdapter(txAdapter, R.string.no_pending_offers)
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
                .bindUntilEvent(lifecycle(), ActivityEvent.DESTROY)
                .subscribe { displayOffers(it) }

        offersRepository.loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .bindUntilEvent(lifecycle(), ActivityEvent.DESTROY)
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

        offersRepository.errorsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .bindUntilEvent(lifecycle(), ActivityEvent.DESTROY)
                .subscribe { error ->
                    if (!txAdapter.hasData) {
                        error_empty_view.showError(error) {
                            update(true)
                        }
                    } else {
                        ErrorHandlerFactory.getDefault().handle(error)
                    }
                }
    }

    private fun displayOffers(items: List<Offer>) {
        txAdapter.setData(
                items
                        .map {
                            MatchTransaction.fromOffer(it)
                        }
                        .map {
                            TxHistoryItem.fromTransaction(it)
                        }
        )
    }

    private fun openDetails(tx: Transaction?) {
        when (tx) {
            is MatchTransaction -> TxDetailsActivity
                    .startForResult<OfferMatchDetailsActivity, MatchTransaction>(
                            this, tx, CANCEL_OFFER_REQUEST)
            else -> (tx as? BaseTransaction)?.let {
                TxDetailsActivity.start<UnknownTxDetailsActivity, BaseTransaction>(this, it)
            }
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
        private val CANCEL_OFFER_REQUEST = "cancel_offer".hashCode() and 0xffff
    }
}
