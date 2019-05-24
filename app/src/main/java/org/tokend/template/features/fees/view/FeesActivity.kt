package org.tokend.template.features.fees.view

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_fees.*
import kotlinx.android.synthetic.main.appbar_white_asset_tab.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.repository.FeesRepository
import org.tokend.template.features.fees.adapter.FeeListItem
import org.tokend.template.features.fees.adapter.FeesAdapter
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.HorizontalSwipesGestureDetector
import org.tokend.template.view.util.LoadingIndicatorManager
import java.lang.ref.WeakReference

class FeesActivity : BaseActivity() {

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val feesRepository: FeesRepository
        get() = repositoryProvider.fees()

    private val assets: Set<String>
        get() = feesRepository.item?.feesAssetMap?.keys ?: emptySet()

    private var asset: String = ""
        set(value) {
            field = value
            updateFeeCards()
        }

    private val requestedAssetCode: String? by lazy {
        intent.getStringExtra(EXTRA_ASSET)
    }

    private val requestedType: Int by lazy {
        intent.getIntExtra(EXTRA_TYPE, -1)
    }

    private var toRequestedAsset = true
    private var toRequestedItems = false

    private val feesAdapter = FeesAdapter()

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_fees)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        subscribeToFees()
        update()
    }

    private fun initViews() {
        initAssetTabs()
        initList()
        initSwipeRefresh()
        initHorizontalSwipes()
        error_empty_view.setEmptyDrawable(R.drawable.ic_flash)
    }

    private fun initList() {
        list_fees.layoutManager = LinearLayoutManager(this)
        list_fees.adapter = feesAdapter
    }

    private fun initAssetTabs() {
        asset_tab_layout.onItemSelected {
            asset = it.text
        }
        asset_tab_layout.visibility = View.GONE
    }

    private fun initHorizontalSwipes() {

        val weakTabs = WeakReference(asset_tab_layout)

        val gestureDetector = GestureDetectorCompat(this, HorizontalSwipesGestureDetector(
                onSwipeToLeft = {
                    weakTabs.get()?.apply { selectedItemIndex++ }
                },
                onSwipeToRight = {
                    weakTabs.get()?.apply { selectedItemIndex-- }
                }
        ))

        swipe_refresh.setTouchEventInterceptor(gestureDetector::onTouchEvent)
        swipe_refresh.setOnTouchListener { _, event ->
            if (error_empty_view.visibility == View.VISIBLE)
                gestureDetector.onTouchEvent(event)

            false
        }
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }

    private var feesDisposable: CompositeDisposable? = null
    private fun subscribeToFees() {
        feesDisposable?.dispose()
        feesDisposable = CompositeDisposable(
                feesRepository.itemSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            onFeesUpdated()
                        },
                feesRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { isLoading ->
                            loadingIndicator.setLoading(isLoading, "fees")
                        },
                feesRepository.errorsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { error ->
                            if (feesRepository.isNeverUpdated) {
                                error_empty_view.showError(error, errorHandlerFactory.getDefault()) {
                                    update(true)
                                }
                            } else {
                                errorHandlerFactory.getDefault().handle(error)
                            }
                        }
        ).also { it.addTo(compositeDisposable) }
    }

    private fun onFeesUpdated() {
        if (assets.isEmpty()) {
            asset_tab_layout.visibility = View.GONE
            error_empty_view.showEmpty(getString(R.string.no_fees))
            return
        } else {
            asset_tab_layout.visibility = View.VISIBLE
            error_empty_view.hide()
        }

        val sortedAssets = assets.sortedWith(assetComparator)
        asset_tab_layout.setSimpleItems(sortedAssets, asset)
        if (toRequestedAsset && requestedAssetCode != null) {
            toRequestedItems = true

            val forceUpdate = asset == requestedAssetCode
            asset_tab_layout.selectedItemIndex =
                    sortedAssets.indexOfFirst { it == requestedAssetCode }

            if (forceUpdate) { updateFeeCards() }

            toRequestedAsset = false
            return
        }
        updateFeeCards()
    }

    private fun updateFeeCards() {
        feesAdapter.clearData()
        feesRepository.item?.feesAssetMap?.get(asset)?.let { fees ->
            val data = fees.map { fee ->
                FeeListItem.fromFee(fee, amountFormatter)
            }.groupBy { "${it.type}:${it.subtype}" }.values

            val sortedData = data.sortedWith(compareBy({ it.first().type }, { it.first().subtype }))

            feesAdapter.setData(sortedData)

            if (toRequestedItems) {
                list_fees.scrollToPosition(
                        sortedData.indexOfFirst { it.first().type.value == requestedType }
                )
                toRequestedItems = false
            }
        }
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            feesRepository.updateIfNotFresh()
        } else {
            feesRepository.update()
        }
    }

    companion object {
        const val EXTRA_ASSET = "extra_asset"
        const val EXTRA_TYPE = "extra_type"
    }
}
