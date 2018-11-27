package org.tokend.template.features.fees

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_fees.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.tokend.template.R
import org.tokend.template.data.repository.FeesRepository
import org.tokend.template.features.fees.adapter.FeeAdapter
import org.tokend.template.features.fees.adapter.FeeItem
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.HorizontalSwipesGestureDetector
import org.tokend.template.view.util.LoadingIndicatorManager
import java.lang.ref.WeakReference

class FeesFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val feesRepository: FeesRepository
        get() = repositoryProvider.fees()

    private val assets: Set<String>
        get() = feesRepository.itemSubject.value.feesAssetMap.keys

    private var asset: String = ""
        set(value) {
            field = value
            onAssetChanged()
        }

    private val feeAdapter = FeeAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_fees, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)
        toolbar.title = getString(R.string.my_fees)

        initViews()
        subscribeToFees()
        update()
    }

    private fun initViews() {
        initFeeList()
        initAssetTabs()
        initSwipeRefresh()
        initHorizontalSwipes()
    }

    private fun initFeeList() {
        error_empty_view.setPadding(0, 0, 0,
                resources.getDimensionPixelSize(R.dimen.quadra_margin))

        fee_list.apply {
            layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = feeAdapter
        }
    }

    private fun initAssetTabs() {
        asset_tabs.onItemSelected {
            asset = it.text
        }
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

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
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
                            if(isLoading) {
                                error_empty_view.hide()
                            }
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
        asset_tabs.setSimpleItems(assets)
        if(assets.isEmpty()) {
            error_empty_view.showEmpty(getString(R.string.no_fees))
        }
    }

    private fun onAssetChanged() {
        feesRepository.itemSubject.value.feesAssetMap[asset]?.let {
            feeAdapter.setData(it.map { FeeItem.fromFee(it) })
        }
    }

    private fun update(force: Boolean = false) {
        if(!force) {
            feesRepository.updateIfNotFresh()
        } else {
            feesRepository.update()
        }
    }

    companion object {
        const val ID = 1120L
        fun newInstance(): FeesFragment {
            val fragment = FeesFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}