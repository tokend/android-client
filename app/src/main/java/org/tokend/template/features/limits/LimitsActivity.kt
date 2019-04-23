package org.tokend.template.features.limits

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
import android.view.View
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_limits.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.repository.LimitsRepository
import org.tokend.template.extensions.isMaxPossibleAmount
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.HorizontalSwipesGestureDetector
import org.tokend.template.view.util.LoadingIndicatorManager
import java.lang.ref.WeakReference

class LimitsActivity : BaseActivity() {

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val limitsRepository: LimitsRepository
        get() = repositoryProvider.limits()

    private val assets: Set<String>
        get() = limitsRepository.item?.entriesByAssetMap?.keys ?: emptySet()

    private var asset: String = ""
        set(value) {
            field = value
            onAssetChanged()
        }

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_limits)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initSwipeRefresh()
        initTabs()
        initEmptyErrorView()
        initHorizontalSwipes()
        subscribeLimits()
        update()
    }

    private fun initTabs() {
        asset_tabs.onItemSelected { item ->
            asset = item.text
        }
    }

    private fun initEmptyErrorView() {
        error_empty_view.setEmptyDrawable(R.drawable.ic_insert_chart)
        error_empty_view.setPadding(0, resources.getDimensionPixelSize(R.dimen.standard_padding), 0, 0)
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
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

        touch_capture_layout.setTouchEventInterceptor(gestureDetector::onTouchEvent)
        swipe_refresh.setOnTouchListener { _, event ->
            if (error_empty_view.visibility == View.VISIBLE)
                gestureDetector.onTouchEvent(event)

            false
        }
    }

    private fun onAssetChanged() {
        updateCards(asset)
    }

    private fun updateCards(asset: String) {
        limit_cards_holder.removeAllViews()

        limitsRepository.item?.getAssetEntries(asset)
                ?.filter {
                    !it.limit.daily.isMaxPossibleAmount()
                            || !it.limit.weekly.isMaxPossibleAmount()
                            || !it.limit.monthly.isMaxPossibleAmount()
                            || !it.limit.annual.isMaxPossibleAmount()
                }
                ?.let { entries ->
                    LimitCardsProvider(this, asset, entries, amountFormatter)
                            .addTo(limit_cards_holder)
                }

        if (limit_cards_holder.childCount == 0) {
            error_empty_view.showEmpty(getString(R.string.no_limits_for_asset_message))
        }
    }

    private fun onLimitsUpdated() {
        if (assets.isNotEmpty()) {
            error_empty_view.hide()
            asset_tabs.visibility = View.VISIBLE
            asset_tabs.setSimpleItems(assets)
            updateCards(asset)
        } else {
            asset_tabs.visibility = View.GONE
            error_empty_view.showEmpty(getString(R.string.no_limits_message))
        }
    }

    private var limitsDisposable: CompositeDisposable? = null
    private fun subscribeLimits() {
        limitsDisposable?.dispose()
        limitsDisposable = CompositeDisposable(
                limitsRepository.itemSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe {
                            onLimitsUpdated()
                        },
                limitsRepository.errorsSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { error ->
                            if (limitsRepository.isNeverUpdated) {
                                error_empty_view.showError(error, errorHandlerFactory.getDefault()) {
                                    update(true)
                                }
                            } else {
                                errorHandlerFactory.getDefault().handle(error)
                            }
                        },
                limitsRepository.loadingSubject
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribe { isLoading ->
                            loadingIndicator.setLoading(isLoading, "limits")
                        }
        ).also { it.addTo(compositeDisposable) }
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            limitsRepository.updateIfNotFresh()
        } else {
            limitsRepository.update()
        }
    }
}
