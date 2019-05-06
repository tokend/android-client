package org.tokend.template.features.dashboard.balances.view

import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.LinearLayoutManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_balances.*
import org.jetbrains.anko.dip
import org.tokend.template.R
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.features.dashboard.balances.view.adapter.BalanceItemsAdapter
import org.tokend.template.features.dashboard.balances.view.adapter.BalanceListItem
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.ObservableTransformers
import java.math.BigDecimal
import java.text.NumberFormat


class BalancesFragment : BaseFragment() {
    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private lateinit var adapter: BalanceItemsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balances, container, false)
    }

    override fun onInitAllowed() {
        initChart()
        initList()

        subscribeToBalances()
    }

    // region Init
    private fun initChart() {
        class DistributionData(
                val name: String,
                val converted: BigDecimal,
                val amount: BigDecimal
        )

        val distributionData = mapOf(
                "USD" to DistributionData("United States Dollar",
                        BigDecimal("2360"), BigDecimal("2360")),
                "BTC" to DistributionData("Bitcoin",
                        BigDecimal("1298"), BigDecimal("0.45")),
                "EUR" to DistributionData("Euro",
                        BigDecimal("1357"), BigDecimal("1103")),
                "" to DistributionData("Other", BigDecimal("885"), BigDecimal.ZERO)
        )


        val entries = listOf(
                PieEntry(40f, "USD"),
                PieEntry(23f, "BTC"),
                PieEntry(22f, "EUR"),
                PieEntry(15f, "")
        )

        val dataset = PieDataSet(entries, null)

        dataset.sliceSpace = 0f
        dataset.colors = listOf(
                "#39A394",
                "#7C73FB",
                "#F0A026",
                "#D6D6D6"
        )
                .map(Color::parseColor)
        dataset.setDrawValues(false)
        dataset.selectionShift = 4f
        dataset.setAutomaticallyDisableSliceSpacing(true)

        val data = PieData(dataset)

        distribution_chart.data = data
        distribution_chart.invalidate()
        distribution_chart.legend.isEnabled = false
        distribution_chart.description = null
        distribution_chart.setUsePercentValues(true)
        distribution_chart.setDrawEntryLabels(false)

        distribution_chart.legend.entries.forEach { legendEntry ->
            val dData = distributionData.getValue(legendEntry.label)

            legend_layout.addView(
                    LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL

                        layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = dip(4)
                        }

                        gravity = Gravity.CENTER_VERTICAL

                        addView(
                                View(requireContext()).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                            requireContext().dip(12),
                                            requireContext().dip(12)
                                    )

                                    background = ShapeDrawable(OvalShape()).apply {
                                        intrinsicWidth = requireContext().dip(12)
                                        intrinsicHeight = requireContext().dip(12)
                                        paint.color = legendEntry.formColor
                                    }
                                }
                        )

                        addView(
                                LinearLayout(requireContext()).apply {
                                    layoutParams = LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                    ).apply {
                                        marginStart = dip(8)
                                    }

                                    orientation = LinearLayout.VERTICAL


                                    addView(
                                            TextView(requireContext()).apply {
                                                text = dData.name
                                            }
                                    )

                                    addView(
                                            TextView(ContextThemeWrapper(requireContext(), R.style.HintText), null, R.style.HintText).apply {
                                                var t = amountFormatter.formatAssetAmount(dData.amount, legendEntry.label)
                                                if (legendEntry.label.isNotEmpty() && legendEntry.label != "USD") {
                                                    t += " / " + amountFormatter.formatAssetAmount(dData.converted, "USD")
                                                }
                                                text = t
                                            }
                                    )
                                }
                        )
                    }
            )
        }

        distribution_chart.holeRadius = 60f
        distribution_chart.transparentCircleRadius = 68f

        distribution_chart.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text))
        distribution_chart.setCenterTextSizePixels(requireContext().resources.getDimensionPixelSize(R.dimen.text_size_default).toFloat())
        val percentFormat = NumberFormat.getPercentInstance()
        var prevHighlight = 0f
        distribution_chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onNothingSelected() {
                distribution_chart.highlightValue(prevHighlight, 0)
            }

            override fun onValueSelected(e: Entry, h: Highlight) {
                distribution_chart.centerText = percentFormat.format(e.y / 100)
                prevHighlight = h.x
            }

        })
        distribution_chart.highlightValue(0f, 0)
    }

    private fun initList() {
        adapter = BalanceItemsAdapter(amountFormatter)
        balances_list.adapter = adapter
        balances_list.layoutManager = LinearLayoutManager(requireContext())
    }
    // endregion

    private fun subscribeToBalances() {
        balancesRepository
                .itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { displayBalances() }
                .addTo(compositeDisposable)
    }

    private fun displayBalances() {
        val items = balancesRepository
                .itemsList
                .map(::BalanceListItem)
                .toMutableList()
        items.addAll(items)
        items.addAll(items)

        adapter.setData(items)
    }
}