package org.tokend.template.features.limits

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_limits.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.tokend.template.R
import org.tokend.template.data.repository.LimitsRepository
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.HorizontalSwipesGestureDetector
import org.tokend.template.view.util.LoadingIndicatorManager
import java.lang.ref.WeakReference

class LimitsFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val limitsRepository: LimitsRepository
        get() = repositoryProvider.limits()

    private val assets: Set<String>
        get() = limitsRepository.itemSubject.value.entriesByAssetMap.keys

    private var asset: String = ""
        set(value) {
            field = value
            onAssetChanged()
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_limits, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)
        toolbar.title = getString(R.string.limits)

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
        error_empty_view.setPadding(0,
                requireContext().resources.getDimensionPixelSize(R.dimen.standard_padding),0 ,0)
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }

    private fun initHorizontalSwipes() {

        val weakTabs = WeakReference(asset_tabs)

        val gestureDetector = GestureDetectorCompat(requireContext(), HorizontalSwipesGestureDetector(
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

        limitsRepository.itemSubject.value.getAssetEntries(asset)?.let { entries ->

            LimitCardsProvider(requireContext(), asset, entries)
                    .addTo(limit_cards_holder)

        }
    }

    private fun onLimitsUpdated() {
        if(assets.isNotEmpty()) {
            error_empty_view.hide()
            asset_tabs.setSimpleItems(assets)
            updateCards(asset)
        } else {
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

    companion object {
        const val ID = 1119L
        fun newInstance(): LimitsFragment {
            val fragment = LimitsFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}