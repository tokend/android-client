package org.tokend.template.features.trade.chart.view

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
import org.tokend.rx.extensions.toSingle
import org.tokend.template.R
import org.tokend.template.data.model.AssetPairRecord
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.ObservableTransformers

class AssetPairChartFragment : BaseFragment() {
    private lateinit var assetPair: AssetPairRecord

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_asset_chart, container, false)
    }

    override fun onInitAllowed() {
        assetPair = arguments?.getSerializable(ASSET_PAIR_EXTRA) as? AssetPairRecord
                ?: return

        initChart()
        update()
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

    private var chartDisposable: Disposable? = null

    private fun update() {
        chartDisposable?.dispose()
        chartDisposable = apiProvider.getApi()
                .assets
                .getChart(assetPair.base, assetPair.quote)
                .toSingle()
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    chart.isLoading = true
                }
                .doOnEvent { _, _ ->
                    chart.isLoading = false
                }
                .subscribeBy(
                        onSuccess = {
                            chart.post {
                                chart.data = it
                            }
                        },
                        onError = {
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
                .addTo(compositeDisposable)
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