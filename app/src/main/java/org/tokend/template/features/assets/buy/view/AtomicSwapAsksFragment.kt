package org.tokend.template.features.assets.buy.view

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_atomic_swap_asks.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.tokend.template.R
import org.tokend.template.data.model.AtomicSwapAskRecord
import org.tokend.template.data.repository.AtomicSwapRequestsRepository
import org.tokend.template.features.assets.buy.view.adapter.AtomicSwapAskListItem
import org.tokend.template.features.assets.buy.view.adapter.AtomicSwapAsksAdapter
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager

class AtomicSwapAsksFragment : BaseFragment() {
    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val assetCode: String by lazy {
        arguments?.getString(ASSET_CODE_EXTRA)
                ?: throw IllegalArgumentException("$ASSET_CODE_EXTRA must be specified in arguments")
    }

    private val asksRepository: AtomicSwapRequestsRepository
        get() = repositoryProvider.atomicSwapAsks(assetCode)

    private lateinit var adapter: AtomicSwapAsksAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_atomic_swap_asks, container, false)
    }

    override fun onInitAllowed() {
        initList()
        initSwipeRefresh()

        subscribeToAsks()

        update()
    }

    // region Init
    private fun initList() {
        adapter = AtomicSwapAsksAdapter(amountFormatter)

        asks_recycler_view.layoutManager = LinearLayoutManager(requireContext())
        asks_recycler_view.adapter = adapter

        error_empty_view.setEmptyDrawable(R.drawable.ic_trade)
        error_empty_view.observeAdapter(adapter, R.string.no_offers)
        error_empty_view.setEmptyViewDenial(asksRepository::isNeverUpdated)
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }
    // endregion

    private fun subscribeToAsks() {
        asksRepository
                .itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { displayAsks() }
                .addTo(compositeDisposable)

        asksRepository
                .loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    loadingIndicator.setLoading(it)
                }
                .addTo(compositeDisposable)

        asksRepository
                .errorsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { error ->
                    if (!adapter.hasData) {
                        error_empty_view.showError(error,
                                errorHandlerFactory.getDefault()) {
                            update(true)
                        }
                    } else {
                        errorHandlerFactory.getDefault().handle(error)
                    }
                }
                .addTo(compositeDisposable)
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            asksRepository.updateIfNotFresh()
        } else {
            asksRepository.update()
        }
    }

    private fun displayAsks() {
        val items = asksRepository
                .itemsList
                .filterNot(AtomicSwapAskRecord::isCanceled)
                .map(::AtomicSwapAskListItem)
        adapter.setData(items)
    }

    companion object {
        private const val ASSET_CODE_EXTRA = "asset_code"

        fun newInstance(assetCode: String): AtomicSwapAsksFragment {
            val fragment = AtomicSwapAsksFragment()
            fragment.arguments = Bundle().apply {
                putString(ASSET_CODE_EXTRA, assetCode)
            }
            return fragment
        }
    }
}