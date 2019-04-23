package org.tokend.template.features.invest.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.listener.BarLineChartTouchListener
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_asset_chart.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.layout_progress.*
import org.jetbrains.anko.dip
import org.tokend.template.R
import org.tokend.template.data.repository.assets.AssetChartRepository
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager

class SaleChartFragment : SaleFragment() {
    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { chart.isLoading = true },
            hideLoading = { chart.isLoading = true }
    )

    private val chartRepository: AssetChartRepository
        get() = repositoryProvider.assetChart(sale.baseAssetCode)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_asset_chart, container, false)
    }

    override fun onInitAllowed() {
        super.onInitAllowed()

        initChart()
        subscribeToChartData()

        error_empty_view.showEmpty(R.string.loading_data)

        chartRepository.update()
    }

    private fun initChart() {
        chart.apply {
            val quoteAsset = sale.defaultQuoteAsset

            this.asset = quoteAsset
            valueHint = getString(R.string.deployed_hint)
            total = sale.currentCap

            setLimitLines(listOf(
                    sale.softCap.toFloat() to
                            amountFormatter.formatAssetAmount(sale.softCap, quoteAsset),
                    sale.hardCap.toFloat() to
                            amountFormatter.formatAssetAmount(sale.hardCap, quoteAsset)
            ))

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
            error_empty_view.showEmpty(R.string.no_sale_chart_yet)
        } else {
            error_empty_view.hide()
            chart.post {
                chart.data = data
            }
        }
    }
}