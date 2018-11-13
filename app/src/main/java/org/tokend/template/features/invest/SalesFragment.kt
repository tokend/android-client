package org.tokend.template.features.invest

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_sales.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.layout_sales_search.view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.dip
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.sales.params.SalesParams
import org.tokend.template.R
import org.tokend.template.base.fragments.BaseFragment
import org.tokend.template.base.fragments.ToolbarProvider
import org.tokend.template.base.view.util.AnimationUtil
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.features.invest.adapter.SalesAdapter
import org.tokend.template.features.invest.repository.SalesRepository
import org.tokend.template.util.Navigator
import org.tokend.template.util.SoftInputUtil
import java.util.concurrent.TimeUnit

class SalesFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val salesRepository: SalesRepository
        get() = repositoryProvider.sales()

    private lateinit var salesAdapter: SalesAdapter

    private var searchItem: MenuItem? = null
    private var nameQuery = ""
    private var tokenQuery = ""
    private val hasFilter: Boolean
        get() = nameQuery.isNotEmpty() || tokenQuery.isNotEmpty()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sales, container, false)
    }

    override fun onInitAllowed() {
        initToolbar()
        initSwipeRefresh()
        initSalesList()

        subscribeToSales()

        update()
    }

    // region Init
    private fun initToolbar() {
        toolbarSubject.onNext(toolbar)
        toolbar.title = getString(R.string.explore_sales_title)
        initMenu()
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }

    private fun initSalesList() {
        salesAdapter = SalesAdapter(urlConfigProvider.getConfig().storage)
        error_empty_view.observeAdapter(salesAdapter, R.string.no_sales_found)
        error_empty_view.setEmptyViewDenial { !hasFilter && salesRepository.isNeverUpdated }

        salesAdapter.onItemClick { _, sale ->
            Navigator.openSale(this, INVESTMENT_REQUEST, sale)
        }

        sales_list.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = salesAdapter

            listenBottomReach({ salesAdapter.getDataItemCount() }) {
                salesRepository.loadMore() || salesRepository.noMoreItems
            }

            setItemViewCacheSize(20)
            isDrawingCacheEnabled = true
            drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
            (sales_list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun initMenu() {
        toolbar.inflateMenu(R.menu.sales)
        val menu = toolbar.menu

        val pendingOffersItem = menu?.findItem(R.id.pending_offers)
        searchItem = menu?.findItem(R.id.search) ?: return
        val searchLayout = searchItem?.actionView as? LinearLayout ?: return

        val nameEditText = searchLayout.search_name_edit_text
        nameEditText.setMetTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        val tokenEditText = searchLayout.search_token_edit_text
        tokenEditText.setMetTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        val updateFilter = {
            val prevNameQuery = nameQuery
            nameQuery = nameEditText.text.toString()
            val prevTokenQuery = tokenQuery
            tokenQuery = tokenEditText.text.toString()

            if (prevNameQuery != nameQuery || prevTokenQuery != tokenQuery) {
                update()
            }
        }

        Observable.merge(
                RxTextView.textChanges(nameEditText).skipInitialValue(),
                RxTextView.textChanges(tokenEditText).skipInitialValue()
        )
                .debounce(600, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    updateFilter()
                }
                .addTo(compositeDisposable)

        searchItem?.setOnActionExpandListener(
                object : MenuItem.OnActionExpandListener {
                    var trueCollapse = false
                    override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                        trueCollapse = false

                        nameEditText?.requestFocus()
                        SoftInputUtil.showSoftInputOnView(nameEditText)
                        pendingOffersItem?.isVisible = false

                        // The only way I found for correct positioning of custom search layout.
                        searchLayout.translationY = requireContext().dip(38).toFloat()

                        AnimationUtil.expandView(searchLayout)

                        return true
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                        if (!trueCollapse) {
                            AnimationUtil.collapseView(searchLayout) {
                                trueCollapse = true
                                searchItem?.collapseActionView()
                            }
                        } else {
                            nameEditText?.text?.clear()
                            tokenEditText?.text?.clear()
                            updateFilter()
                            SoftInputUtil.hideSoftInput(requireActivity())
                            pendingOffersItem?.isVisible = true
                        }

                        return trueCollapse
                    }
                })

        pendingOffersItem?.setOnMenuItemClickListener {
            Navigator.openPendingOffers(this, CANCEL_OFFER_REQUEST, true)
            true
        }
    }
    // endregion

    private var salesDisposable: CompositeDisposable? = null
    private fun subscribeToSales() {
        if (salesDisposable?.isDisposed == false) {
            return
        }

        unsubscribeFromSales()

        salesDisposable = CompositeDisposable(
                salesRepository.loadingSubject
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnDispose {
                            swipe_refresh.isRefreshing = false
                            salesAdapter.hideLoadingFooter()
                        }
                        .subscribe { loading ->
                            if (loading) {
                                if (salesRepository.isOnFirstPage) {
                                    swipe_refresh.isRefreshing = true
                                } else {
                                    salesAdapter.showLoadingFooter()
                                }
                            } else {
                                swipe_refresh.isRefreshing = false
                                salesAdapter.hideLoadingFooter()
                            }
                        },
                salesRepository.errorsSubject
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { handleSalesError(it) },
                salesRepository.itemsSubject
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            salesAdapter.setData(it)
                        }
        ).also { it.addTo(compositeDisposable) }
    }

    private fun unsubscribeFromSales() {
        salesDisposable?.dispose()
    }

    private fun handleSalesError(error: Throwable) {
        if (!salesAdapter.hasData) {
            error_empty_view.showError(error, errorHandlerFactory.getDefault()) {
                update()
            }
        } else {
            errorHandlerFactory.getDefault().handle(error)
        }
    }

    private fun update(force: Boolean = false) {
        if (!hasFilter) {
            filterDisposable?.dispose()
            subscribeToSales()
            if (force) {
                salesRepository.update()
            } else {
                salesRepository.updateIfNotFresh()
            }
        } else {
            unsubscribeFromSales()
            filter()
        }
    }

    private var filterDisposable: Disposable? = null

    private fun filter() {
        filterDisposable?.dispose()
        filterDisposable =
                salesRepository
                        .getPage(SalesParams(
                                name = nameQuery,
                                baseAsset = tokenQuery,
                                pagingParams = PagingParamsV2(
                                        limit = 250,
                                        order = PagingOrder.DESC
                                )
                        ))
                        .map { it.items }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe {
                            loadingIndicator.show()
                        }
                        .doOnEvent { _, _ ->
                            loadingIndicator.hide()
                        }
                        .subscribeBy(
                                onSuccess = { salesAdapter.setData(it) },
                                onError = { handleSalesError(it) }
                        )
                        .addTo(compositeDisposable)
    }

    override fun onBackPressed(): Boolean {
        return searchItem?.isActionViewExpanded == false.also {
            searchItem?.collapseActionView()
        }
    }

    companion object {
        val ID = "sales_fragment".hashCode().toLong() and 0xffff
        private val INVESTMENT_REQUEST = "invest".hashCode() and 0xfff
        private val CANCEL_OFFER_REQUEST = "cancel_offer".hashCode() and 0xffff
    }
}