package org.tokend.template.features.trade.chart.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.listener.BarLineChartTouchListener
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_asset_chart.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.jetbrains.anko.dip
import org.tokend.template.R
import org.tokend.template.data.model.AssetPairRecord
import org.tokend.template.data.repository.assets.AssetChartRepository
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager

class AssetPairChartFragment : BaseFragment() {
    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { chart.isLoading = true },
            hideLoading = { chart.isLoading = false }
    )

    private lateinit var assetPair: AssetPairRecord

    private val chartRepository: AssetChartRepository
        get() = repositoryProvider.assetChart(assetPair.base, assetPair.quote)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_asset_chart, container, false)
    }

    override fun onInitAllowed() {
        assetPair = arguments?.getSerializable(ASSET_PAIR_EXTRA) as? AssetPairRecord
                ?: return

        initChart()
        subscribeToChartData()

        error_empty_view.showEmpty(R.string.loading_data)

        chartRepository.update()
    }

    private fun initChart() {
        chart.apply {
            post {
                asset = assetPair.quote
                total = assetPair.price
                valueHint = getString(R.string.asset_price_for_one_part, assetPair.base)

                valueTextSizePx = this@AssetPairChartFragment.requireContext()
                        .resources.getDimension(R.dimen.text_size_heading_large)

                // Drag fix for nesting inside a ViewPager.
                chartView.onTouchListener = object : BarLineChartTouchListener(
                        chartView,
                        chartView.viewPortHandler.matrixTouch,
                        context.dip(4).toFloat()
                ) {
                    override fun onTouch(v: View, event: MotionEvent): Boolean {
                        return when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                v.parent.requestDisallowInterceptTouchEvent(true)
                                super.onTouch(v, event)
                            }
                            MotionEvent.ACTION_UP -> {
                                v.parent.requestDisallowInterceptTouchEvent(false)
                                super.onTouch(v, event)
                            }
                            MotionEvent.ACTION_MOVE -> {
                                v.parent.requestDisallowInterceptTouchEvent(true)
                                super.onTouch(v, event)
                            }
                            else -> super.onTouch(v, event)
                        }
                    }
                }
            }
        }
    }

    private fun subscribeToChartData() {
        chartRepository
                .itemSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    displayChartData()
                }
                .addTo(compositeDisposable)

        chartRepository
                .loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { loadingIndicator.setLoading(it) }
                .addTo(compositeDisposable)
    }

    private fun displayChartData() {
        val data = chartRepository.item?.takeIf { !it.isEmpty }

        if (data == null && !chartRepository.isNeverUpdated) {
            error_empty_view.showEmpty(R.string.no_price_chart_yet)
        } else {
            error_empty_view.hide()
            chart.post {
                chart.data = data
            }
        }
    }

    companion object {
        private const val ASSET_PAIR_EXTRA = "asset_pair"

        fun newInstance(assetPair: AssetPairRecord): AssetPairChartFragment {
            val fragment = AssetPairChartFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(ASSET_PAIR_EXTRA, assetPair)
            }
            return fragment
        }
    }
}