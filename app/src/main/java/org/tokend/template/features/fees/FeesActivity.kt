package org.tokend.template.features.fees

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
import android.view.View
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_fees.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.repository.FeesRepository
import org.tokend.template.features.fees.model.FeeRecord
import org.tokend.template.features.fees.view.FeeItem
import org.tokend.template.features.fees.view.FeeCard
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.HorizontalSwipesGestureDetector
import org.tokend.template.view.util.LoadingIndicatorManager
import java.lang.ref.WeakReference
import java.math.BigDecimal

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
        initSwipeRefresh()
        initHorizontalSwipes()
        error_empty_view.setEmptyDrawable(R.drawable.ic_flash)
    }

    private fun initAssetTabs() {
        asset_tabs.onItemSelected {
            asset = it.text
        }
        asset_tabs.visibility = View.GONE
    }

    private fun initHorizontalSwipes() {

        val weakTabs = WeakReference(asset_tabs)

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
        asset_tabs.setSimpleItems(assets.sortedWith(assetComparator))
        if (assets.isEmpty()) {
            asset_tabs.visibility = View.GONE
            error_empty_view.showEmpty(getString(R.string.no_fees))
        } else {
            asset_tabs.visibility = View.VISIBLE
            error_empty_view.hide()
            updateFeeCards()
        }
    }

    private fun updateFeeCards() {
        fee_container.removeAllViews()
        feesRepository.item?.feesAssetMap?.get(asset)?.let { fees ->
            val data = fees.map { fee ->
                FeeItem.fromFee(fee, amountFormatter)
            }.groupBy { "${it.type}:${it.subtype}" }.values

            val sortedData = data.sortedWith(compareBy({ it.first().type }, { it.first().subtype }))

            sortedData.forEach {
                FeeCard(this, it).addTo(fee_container)
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
}
