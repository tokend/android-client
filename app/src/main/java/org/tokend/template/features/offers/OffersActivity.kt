package org.tokend.template.features.offers

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_offers.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.data.repository.offers.OffersRepository
import org.tokend.template.features.offers.view.PendingOfferListItem
import org.tokend.template.features.offers.view.PendingOffersAdapter
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager

class OffersActivity : BaseActivity() {
    private val onlyPrimary: Boolean
        get() = intent.getBooleanExtra(ONLY_PRIMARY_EXTRA, false)

    private val offersRepository: OffersRepository
        get() = repositoryProvider.offers(onlyPrimary)

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private lateinit var adapter: PendingOffersAdapter

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
        adapter = PendingOffersAdapter(amountFormatter, false)
        adapter.onItemClick { _, item ->
            item.source?.also {
                Navigator.openPendingOfferDetails(this, it)
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
        history_list.layoutManager = LinearLayoutManager(this)

        history_list.listenBottomReach({ adapter.getDataItemCount() }) {
            offersRepository.loadMore() || offersRepository.noMoreItems
        }

        date_text_switcher.init(history_list, adapter)
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
                        .map {
                            PendingOfferListItem(it)
                        }
        )
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            offersRepository.updateIfNotFresh()
        } else {
            offersRepository.update()
        }
    }

    companion object {
        const val ONLY_PRIMARY_EXTRA = "only_primary"
    }
}
