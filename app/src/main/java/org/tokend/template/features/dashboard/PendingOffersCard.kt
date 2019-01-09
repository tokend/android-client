package org.tokend.template.features.dashboard

import android.content.Context
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
import kotlinx.android.synthetic.main.layout_pending_offers_card.view.*
import kotlinx.android.synthetic.main.layout_progress.view.*
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.base.model.operations.OfferMatchOperation
import org.tokend.template.R
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.data.repository.base.MultipleItemsRepository
import org.tokend.template.view.adapter.history.TxHistoryAdapter
import org.tokend.template.view.adapter.history.TxHistoryItem
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.ViewProvider
import org.tokend.template.data.repository.offers.OffersRepository
import org.tokend.template.util.Navigator
import org.tokend.template.view.util.formatter.AmountFormatter

class PendingOffersCard(private val context: Context?,
                        private val repositoryProvider: RepositoryProvider,
                        private val disposable: CompositeDisposable,
                        private val amountFormatter: AmountFormatter) : ViewProvider {

    private lateinit var view: View

    private lateinit var loadingIndicator: LoadingIndicatorManager

    private val offersRepository: OffersRepository
        get() = repositoryProvider.offers()

    private val offersAdapter = TxHistoryAdapter(true)

    override fun addTo(rootView: ViewGroup): PendingOffersCard {
        rootView.addView(getView(rootView))

        return this
    }

    override fun getView(rootView: ViewGroup): View {
        view = LayoutInflater.from(context)
                .inflate(R.layout.layout_pending_offers_card, rootView, false)

        initPendingOffers()
        initLoadingManager()
        subscribeToOffers()

        return view
    }

    fun initViewMoreButton(fragment: Fragment) {
        view.view_more_offers_button.onClick {
            Navigator.openPendingOffers(fragment, CANCEL_OFFER_REQUEST)
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
                        emptyView.text = context?.getString(R.string.loading_data)
                    } else {
                        emptyView.text = emptyText
                    }
                    emptyView.visibility = View.VISIBLE
                    contentView.visibility = View.GONE
                }
            }
        }
    }

    private fun initPendingOffers() {
        offersAdapter.amountFormatter = amountFormatter
        offersAdapter.registerAdapterDataObserver(
                getEmptyViewObserver(view.offers_empty_view,
                        context?.getString(R.string.no_pending_offers),
                        view.offers_layout) {
                    offersRepository
                }
        )

        view.offers_list.layoutManager = LinearLayoutManager(context)
        view.offers_list.adapter = offersAdapter
        view.offers_list.isNestedScrollingEnabled = false
    }

    private fun initLoadingManager() {
        loadingIndicator = LoadingIndicatorManager(
                showLoading = { view.progress.show() },
                hideLoading = { view.progress.hide() }
        )
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
                                        OfferMatchOperation.fromOffer(it)
                                )
                            })
                        },
                offersRepository.loadingSubject
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { loading ->
                            loadingIndicator.setLoading(loading)
                            if (loading) {
                                view.offers_empty_view.text = context?.getString(R.string.loading_data)
                            }
                        }
        ).also { it.addTo(disposable) }
    }

    companion object {
        private const val TRANSACTIONS_TO_DISPLAY = 3
        val CANCEL_OFFER_REQUEST = "cancel_offer".hashCode() and 0xffff
    }
}