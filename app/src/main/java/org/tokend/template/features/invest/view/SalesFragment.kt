package org.tokend.template.features.invest.view

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
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
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_sales.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.layout_sales_search.view.*
import kotlinx.android.synthetic.main.toolbar_white.*
import org.jetbrains.anko.dip
import org.tokend.template.R
import org.tokend.template.features.invest.logic.SalesSubscriptionManager
import org.tokend.template.features.invest.repository.SalesRepository
import org.tokend.template.features.invest.view.adapter.SalesAdapter
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.util.Navigator
import org.tokend.template.view.util.AnimationUtil
import org.tokend.template.view.util.ColumnCalculator
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.input.SoftInputUtil
import java.util.concurrent.TimeUnit

class SalesFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val salesRepository: SalesRepository
        get() = repositoryProvider.sales()
    private val filterSalesRepository: SalesRepository
        get() = repositoryProvider.filteredSales()

    private lateinit var salesAdapter: SalesAdapter

    private var searchItem: MenuItem? = null
    private var nameQuery = ""
    private var tokenQuery = ""
    private val hasFilter: Boolean
        get() = nameQuery.isNotEmpty() || tokenQuery.isNotEmpty()

    private lateinit var salesSubscriptionManager: SalesSubscriptionManager
    private lateinit var layoutManager: GridLayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sales, container, false)
    }

    override fun onInitAllowed() {
        initToolbar()
        initSwipeRefresh()
        initSalesList()
        initSubscriptionManager()

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
        val columns = ColumnCalculator.getColumnCount(requireActivity())

        layoutManager = GridLayoutManager(context, columns)

        salesAdapter = SalesAdapter(urlConfigProvider.getConfig().storage, amountFormatter)
        error_empty_view.setEmptyDrawable(R.drawable.ic_invest)
        error_empty_view.observeAdapter(salesAdapter, R.string.no_sales_found)

        salesAdapter.onItemClick { _, sale ->
            Navigator.from(this).openSale(INVESTMENT_REQUEST, sale)
        }

        sales_list.apply {
            layoutManager = this@SalesFragment.layoutManager
            adapter = salesAdapter

            setItemViewCacheSize(20)
            (sales_list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }

        ElevationUtil.initScrollElevation(sales_list, appbar_elevation_view)
    }

    private fun initSubscriptionManager() {
        salesSubscriptionManager = SalesSubscriptionManager(
                sales_list,
                salesAdapter,
                loadingIndicator,
                error_empty_view,
                compositeDisposable,
                errorHandlerFactory
        ) {
            update()
        }
    }

    private fun initMenu() {
        toolbar.inflateMenu(R.menu.sales)
        val menu = toolbar.menu

        val pendingOffersItem = menu?.findItem(R.id.pending_offers)
        searchItem = menu?.findItem(R.id.search) ?: return
        val searchLayout = searchItem?.actionView as? LinearLayout ?: return

        val nameEditText = searchLayout.search_name_edit_text
        val tokenEditText = searchLayout.search_token_edit_text

        val updateFilter = {
            val prevNameQuery = nameQuery
            nameQuery = nameEditText.text.toString()
            val prevTokenQuery = tokenQuery
            tokenQuery = tokenEditText.text.toString()

            sales_list?.scrollToPosition(0)

            if (prevNameQuery != nameQuery && nameQuery.isNotEmpty()
                    || prevTokenQuery != tokenQuery && tokenQuery.isNotEmpty()) {
                update(true)
            } else {
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
            Navigator.from(this).openPendingOffers(CANCEL_OFFER_REQUEST, true)
            true
        }
    }
    // endregion

    private fun update(force: Boolean = false) {
        if (!hasFilter) {
            salesSubscriptionManager
                    .subscribeTo(
                            repository = salesRepository,
                            force = force)
        } else {
            salesSubscriptionManager
                    .subscribeTo(
                            filterSalesRepository,
                            nameQuery,
                            tokenQuery,
                            force
                    )
        }
    }

    override fun onBackPressed(): Boolean {
        return searchItem?.isActionViewExpanded == false.also {
            searchItem?.collapseActionView()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                INVESTMENT_REQUEST,
                CANCEL_OFFER_REQUEST -> {
                    update(force = true)
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        layoutManager.spanCount = ColumnCalculator.getColumnCount(requireActivity())
    }

    companion object {
        val ID = "sales_fragment".hashCode().toLong() and 0xffff
        private val INVESTMENT_REQUEST = "invest".hashCode() and 0xfff
        private val CANCEL_OFFER_REQUEST = "cancel_offer".hashCode() and 0xffff
    }
}