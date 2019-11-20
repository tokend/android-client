package org.tokend.template.features.dashboard.balances.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.support.v7.view.ContextThemeWrapper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import org.jetbrains.anko.dip
import org.jetbrains.anko.layoutInflater
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.App
import org.tokend.template.R
import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.view.ErrorEmptyView
import org.tokend.template.view.util.formatter.AmountFormatter
import java.math.BigDecimal
import java.math.MathContext
import java.text.NumberFormat
import javax.inject.Inject

/**
 * Displays pie chart for asset distribution
 * based on balances converted amounts
 */
class AssetDistributionChart
@JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    @Inject
    lateinit var amountFormatter: AmountFormatter

    private val distribution = ArrayList<AssetDistributionEntry>()

    private inner class AssetDistributionEntry(
            val name: String,
            val assetCode: String?,
            val conversionAsset: Asset,
            val convertedAmount: BigDecimal,
            val percentOfTotal: Float
    ) {
        constructor(balance: BalanceRecord, total: BigDecimal) : this(
                name = balance.asset.name ?: balance.assetCode,
                assetCode = balance.assetCode,
                convertedAmount = balance.convertedAmount!!,
                conversionAsset = balance.conversionAsset!!,
                percentOfTotal = balance.convertedAmount!!.percentOf(total)
        )
    }

    private val chart: PieChart
    private val legendLayout: LinearLayout
    private val emptyView: ErrorEmptyView

    val isEmpty: Boolean
        get() = emptyView.visibility == View.VISIBLE

    // region Init
    init {
        (context.applicationContext as App).stateComponent.inject(this)

        isBaselineAligned = false
        orientation = HORIZONTAL
        context.layoutInflater.inflate(R.layout.layout_asset_distribution_chart,
                this, true)

        chart = findViewById(R.id.chart)
        legendLayout = findViewById(R.id.legend_layout)
        emptyView = findViewById(R.id.error_empty_view)

        initChart()
    }

    private fun initChart() {
        chart.apply {
            legend.isEnabled = false
            description = null
            setDrawEntryLabels(false)

            setUsePercentValues(true)

            holeRadius = 60f
            transparentCircleRadius = 68f

            setCenterTextColor(ContextCompat.getColor(context, R.color.primary_text))
            setCenterTextSizePixels(context.resources.getDimensionPixelSize(R.dimen.text_size_default).toFloat())

            isRotationEnabled = true

            setNoDataText(null)
        }

        val percentFormatter = NumberFormat.getPercentInstance()
        percentFormatter.maximumFractionDigits = 1
        var prevHighlightValue = 0f

        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onNothingSelected() {
                chart.highlightValue(prevHighlightValue, 0)
            }

            override fun onValueSelected(e: Entry, h: Highlight) {
                chart.centerText =
                        if (e.y < 0.1) "< 0.1%"
                        else percentFormatter.format(e.y / 100)
                setLegendEntryHighlight(prevHighlightValue.toInt(), false)
                setLegendEntryHighlight(h.x.toInt(), true)
                prevHighlightValue = h.x
            }
        })
    }
    // endregion

    fun setData(balances: Collection<BalanceRecord>,
                conversionAsset: Asset,
                animate: Boolean = false) {
        val distribution = getDistribution(balances, conversionAsset)

        if (distribution.isEmpty()) {
            showEmpty()
            return
        } else {
            emptyView.hide()
        }

        this.distribution.clear()
        this.distribution.addAll(distribution)
        displayDistribution(distribution, animate)
    }

    // region Display
    private fun showEmpty() {
        emptyView.showEmpty(R.string.no_data)
    }

    private fun displayDistribution(distribution: List<AssetDistributionEntry>,
                                    animate: Boolean) {
        val entries = distribution
                .map { PieEntry(it.percentOfTotal, it.assetCode) }

        val dataSet = PieDataSet(entries, null).apply {
            sliceSpace = 0f
            setDrawValues(false)
            selectionShift = 4f

            if (distribution.size == DISPLAY_COUNT + 1 && distribution.last().assetCode == null) {
                setColors(
                        *PALETTE.subList(0, DISPLAY_COUNT).toIntArray(),
                        OTHER_COLOR
                )
            } else {
                colors = PALETTE
            }
        }

        chart.apply {
            data = PieData(dataSet)
            invalidate()

            if (animate) {
                val animationDuration =
                        context.resources.getInteger(android.R.integer.config_longAnimTime)
                animateXY(
                        animationDuration,
                        animationDuration,
                        Easing.EasingOption.EaseInOutCubic,
                        Easing.EasingOption.EaseInOutCubic
                )
            }

            highlightValue(0f, 0)
        }

        displayLegend(distribution)
        setLegendEntryHighlight(0, true)
    }

    // region Legend
    private fun displayLegend(distribution: List<AssetDistributionEntry>) {
        legendLayout.removeAllViews()

        val distributionMap = distribution
                .associateBy(AssetDistributionEntry::assetCode)

        chart.legend.entries.forEachIndexed { i, legendEntry ->
            val distributionEntry = distributionMap[legendEntry.label]
                    ?: return@forEachIndexed

            legendLayout.addView(
                    getLegendEntryView(i, distributionEntry, legendEntry.formColor)
            )
        }
    }

    private fun getLegendEntryView(index: Int,
                                   distributionEntry: AssetDistributionEntry,
                                   @ColorInt
                                   color: Int): View {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL

            layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                if (index != 0) {
                    topMargin = context.resources.getDimensionPixelSize(R.dimen.quarter_standard_margin)
                }
            }

            gravity = Gravity.CENTER_VERTICAL

            // Color circle.
            val circleSize = context.dip(12)
            addView(
                    View(context).apply {
                        id = getLegendEntryIndicatorId(index)

                        layoutParams = ViewGroup.LayoutParams(
                                circleSize,
                                circleSize
                        )

                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setStroke(dip(2), color)
                        }
                        tag = color
                    }
            )

            // Text.
            addView(
                    LinearLayout(context).apply {
                        layoutParams = LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginStart = context.resources.getDimensionPixelSize(R.dimen.half_standard_margin)
                        }

                        orientation = VERTICAL

                        // Name.
                        addView(
                                TextView(context).apply {
                                    text = distributionEntry.name
                                }
                        )

                        // Amount.
                        addView(
                                TextView(ContextThemeWrapper(context, R.style.HintText), null, R.style.HintText).apply {
                                    text = amountFormatter.formatAssetAmount(
                                            distributionEntry.convertedAmount,
                                            distributionEntry.conversionAsset,
                                            abbreviation = true
                                    )
                                }
                        )
                    }
            )

            setOnClickListener {
                chart.highlightValue(index.toFloat(), 0)
            }
        }
    }

    private fun setLegendEntryHighlight(index: Int, highlight: Boolean) {
        val view = legendLayout.findViewById<View>(getLegendEntryIndicatorId(index))
                ?: return
        val background = (view.background as? GradientDrawable)
                ?: return
        val color = view.tag as? Int
                ?: return

        if (highlight) {
            background.setColor(color)
        } else {
            background.setColor(Color.TRANSPARENT)
        }
    }

    private fun getLegendEntryIndicatorId(index: Int): Int {
        return 7 + index
    }
    // endregion
    // endregion

    // region Calculation
    private fun getDistribution(balances: Collection<BalanceRecord>,
                                conversionAsset: Asset): List<AssetDistributionEntry> {
        val sortedAndFilteredByConverted = balances
                .filter {
                            it.conversionAsset == conversionAsset
                            && (it.convertedAmount?.signum() ?: 0) > 0
                }
                .sortedByDescending { it.convertedAmount!! }

        val total = sortedAndFilteredByConverted.convertedTotal()

        if (total.signum() == 0) {
            return emptyList()
        }

        val result = mutableListOf<AssetDistributionEntry>()

        // Display as many top assets as required or available.
        while (result.size < DISPLAY_COUNT
                && sortedAndFilteredByConverted.size - result.size > 0) {
            val balance = sortedAndFilteredByConverted[result.size]

            result.add(AssetDistributionEntry(balance, total))
        }

        // Add "Other" if possible
        if (result.size == DISPLAY_COUNT
                && sortedAndFilteredByConverted.size - result.size > 1) {
            val otherTotal = sortedAndFilteredByConverted
                    .subList(result.size, sortedAndFilteredByConverted.size)
                    .convertedTotal()

            result.add(
                    AssetDistributionEntry(
                            name = context.getString(R.string.asset_distribution_other),
                            assetCode = null,
                            convertedAmount = otherTotal,
                            conversionAsset = conversionAsset,
                            percentOfTotal = otherTotal.percentOf(total)
                    )
            )
        }
        // If the only balance remained then add it instead of "Other"
        else if (result.size == DISPLAY_COUNT
                && sortedAndFilteredByConverted.size - result.size == 1) {
            val balance = sortedAndFilteredByConverted.last()

            result.add(AssetDistributionEntry(balance, total))
        }

        return result
    }

    private fun Collection<BalanceRecord>.convertedTotal(): BigDecimal =
            fold(BigDecimal.ZERO) { sum, balance ->
                sum.add(balance.convertedAmount ?: BigDecimal.ZERO)
            }

    private fun BigDecimal.percentOf(total: BigDecimal): Float =
            BigDecimalUtil.scaleAmount(
                    this.multiply(BigDecimal(100)).divide(total, MathContext.DECIMAL128),
                    1
            ).toFloat()
    // endregion

    companion object {
        // Count of assets to display without "Other".
        // Must be synced with palette!
        private const val DISPLAY_COUNT = 3

        private val PALETTE = listOf(
                "#39A394",
                "#7C73FB",
                "#F0A026",
                "#EC5454"
        )
                .map(Color::parseColor)

        private val OTHER_COLOR = Color.parseColor("#D6D6D6")
    }
}