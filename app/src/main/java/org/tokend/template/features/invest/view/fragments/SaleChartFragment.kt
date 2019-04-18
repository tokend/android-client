package org.tokend.template.features.invest.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.listener.BarLineChartTouchListener
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_asset_chart.*
import org.jetbrains.anko.dip
import org.tokend.template.R
import org.tokend.template.util.ObservableTransformers

class SaleChartFragment : SaleFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_asset_chart, container, false)
    }

    override fun onInitAllowed() {
        super.onInitAllowed()

        initChart()
        update()
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

    private var chartDisposable: Disposable? = null

    private fun update() {
        chartDisposable?.dispose()

        chartDisposable = investmentInfoRepository
                .getChart(apiProvider.getApi())
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    chart.isLoading = true
                }
                .doOnEvent { _, _ ->
                    chart.isLoading = false
                }
                .subscribeBy(
                        onSuccess = { data ->
                            chart.post {
                                chart.data = data
                            }
                        },
                        onError = { }
                )
                .addTo(compositeDisposable)
    }
}