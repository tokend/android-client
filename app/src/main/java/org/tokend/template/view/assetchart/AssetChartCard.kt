package org.tokend.template.view.assetchart

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.EntryXComparator
import com.google.android.material.tabs.TabLayout
import org.jetbrains.anko.dip
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.App
import org.tokend.template.R
import org.tokend.template.features.assets.model.Asset
import org.tokend.template.features.assets.model.AssetChartData
import org.tokend.template.view.ContentLoadingProgressBar
import org.tokend.template.view.util.LocalizedName
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.template.view.util.formatter.DateFormatters
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.*
import javax.inject.Inject

/**
 * Interactive chart of the asset price
 */
class AssetChartCard : LinearLayout {
    companion object {
        private const val X_LABELS_COUNT = 5
        private const val CHART_LINE_WIDTH = 2f
        private const val HIGHLIGHT_LINE_WIDTH = 2f
    }

    constructor(context: Context, attributeSet: AttributeSet?) :
            super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, style: Int) :
            super(context, attributeSet, style)

    constructor(context: Context) : super(context)

    private val localizedName = LocalizedName(context)

    @Inject
    lateinit var amountFormatter: AmountFormatter

    private var chartScale = AssetChartScale.DAY
        set(value) {
            field = value
            onScaleChanged()
        }
    private var passScrollToChart = false
    private var chartWindowXOffset: Float = 0f
    private val chartData = Collections.synchronizedList(mutableListOf<Entry>())

    private val chartScaleTabs: TabLayout
    private val chart: LineChart
    private val valueTextView: TextView
    private val valueHintTextView: TextView
    private val growthTextView: TextView
    private val growthHintTextView: TextView
    private val progressBar: ContentLoadingProgressBar

    init {
        (context.applicationContext as App).stateComponent.inject(this)
        removeAllViews()
        LayoutInflater.from(context).inflate(R.layout.layout_asset_chart, this, true)

        chartScaleTabs = find(R.id.chart_scale_tabs)
        chart = find(R.id.asset_line_chart)
        valueTextView = find(R.id.issued_text_view)
        valueHintTextView = find(R.id.issued_hint_text_view)
        growthTextView = find(R.id.growth_text_view)
        growthHintTextView = find(R.id.growth_hint_text_view)
        progressBar = find(R.id.progress)

        initChart()
    }

    // region Chart
    private fun initChart() {
        chartScaleTabs.apply {
            AssetChartScale.values().forEach {
                addTab(newTab().setTag(it).setText(localizedName.forAssetChartScaleShort(it)))
                if (it == chartScale) {
                    getTabAt(tabCount - 1)?.select()
                }
            }
        }
        chartScaleTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                (tab?.tag as? AssetChartScale)?.let { chartScale = it }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
        })

        val colorPrimaryHalf =
                ContextCompat.getColor(context, R.color.secondary_text)
        chart.apply {
            setNoDataText(context.getString(R.string.loading_data))
            setNoDataTextColor(colorPrimaryHalf)

            setTouchEnabled(true)
            setDrawGridBackground(false)

            description.isEnabled = false
            axisRight.isEnabled = false

            isHighlightPerTapEnabled = false

            with(xAxis) {
                setValueFormatter { value, _ ->
                    val i = value.toInt()
                    if (i == 0 || i >= chartData.size - 1) {
                        ""
                    } else {
                        val entry = chartData[i]
                        chartScale.dateFormat.format(entry.data as Date)
                    }
                }

                setLabelCount(X_LABELS_COUNT, true)
                textColor = colorPrimaryHalf
                position = XAxis.XAxisPosition.BOTTOM

                setDrawGridLines(false)
                setDrawAxisLine(false)
            }

            with(axisLeft) {
                setDrawGridLines(false)
                disableGridDashedLine()
                setDrawLabels(false)
                setDrawAxisLine(false)
            }

            setViewPortOffsets(0f, 0f, 0f, dip(20).toFloat())

            setDrawMarkers(false)
            legend.isEnabled = false

            setScaleEnabled(false)

            setDrawBorders(false)

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onNothingSelected() {
                    displayTotalValue()
                }

                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e != null) {
                        displayHighlightedValue(e.y, e.data as Date)
                    }
                }
            })

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        passScrollToChart = true
                    }
                    MotionEvent.ACTION_UP -> {
                        passScrollToChart = false
                        displayTotalValue()
                        highlightValue(null)
                    }
                }

                false
            }
        }
    }

    private fun onScaleChanged() {
        if (data != null) {
            updateChart()
        }
    }

    private fun updateChart() {
        val newChartData = mutableListOf<Entry>()

        val points = when (chartScale) {
            AssetChartScale.HOUR -> data?.hour
            AssetChartScale.DAY -> data?.day
            AssetChartScale.MONTH -> data?.month
            AssetChartScale.YEAR -> data?.year
        } ?: emptyList()

        val displayCount = Math.min(points.size, chartScale.pointsToDisplay)

        if (points.isNotEmpty()) {
            val step =
                    if (displayCount != 0)
                        points.size / displayCount * -1
                    else
                        1

            val range = IntProgression.fromClosedRange(points.size - 1, 0, step)
            for (i in range) {
                val point = points[i]
                newChartData.add(Entry(0f, point.value.toFloat(), point.date))
            }
        }

        newChartData.forEachIndexed { i, it ->
            it.x = (newChartData.size - 1 - i).toFloat()
        }
        Collections.sort(newChartData, EntryXComparator())

        chartData.clear()
        chartData.addAll(newChartData)

        val startValue = points.firstOrNull()?.value ?: BigDecimal.ZERO
        val finalValue = points.lastOrNull()?.value ?: BigDecimal.ZERO
        val growth = finalValue - startValue

        val percentGrowth =
                if (startValue.signum() == 0)
                    null
                else
                    growth
                            .divide(startValue, MathContext.DECIMAL128)
                            .multiply(BigDecimal(100))
                            .setScale(2, BigDecimal.ROUND_HALF_UP)

        if (points.isNotEmpty() && finalValue > total) {
            total = finalValue
        }

        drawChartData()
        displayTotalValue()
        displayGrowth(growth, percentGrowth)
    }

    private fun drawChartData() {
        if (chartData.isEmpty()) {
            chart.setNoDataText(context.getString(R.string.no_data))
            chart.clear()
            return
        }

        val dataSet = LineDataSet(chartData, "")

        with(dataSet) {
            color = ContextCompat.getColor(context, R.color.accent)
            lineWidth = CHART_LINE_WIDTH

            setDrawFilled(true)
            if (Build.VERSION.SDK_INT >= 18) {
                fillDrawable =
                        ContextCompat.getDrawable(context,
                                R.drawable.dashboard_chart_fill)
            } else {
                fillColor = ContextCompat.getColor(context, R.color.accent)
            }

            mode = LineDataSet.Mode.HORIZONTAL_BEZIER

            setDrawCircles(false)
            setDrawValues(false)

            setDrawHorizontalHighlightIndicator(false)
            highLightColor =
                    ContextCompat.getColor(context, R.color.secondary_text)
            highlightLineWidth = HIGHLIGHT_LINE_WIDTH
        }

        var yMin = dataSet.yMin * 0.85f
        if (yMin < 0f) {
            yMin = 0f
        }

        var yMax = dataSet.yMax * 1.15f
        chart.axisLeft.limitLines.forEach { line ->
            if (yMax < line.limit) {
                yMax = line.limit
                return@forEach
            }
        }

        chart.post {
            with(chart) {
                axisLeft.axisMinimum = yMin
                axisLeft.axisMaximum = yMax

                data = LineData(dataSet)
                invalidate()
            }
        }
    }

    private fun displayTotalValue() {
        val asset = this.asset ?: return
        valueTextView.text =
                amountFormatter.formatAssetAmount(total, asset)
        valueHintTextView.text = valueHint
    }

    private fun displayHighlightedValue(count: Float, date: Date) {
        val amount =
                BigDecimalUtil.stripTrailingZeros(
                        BigDecimal(count.toDouble()).setScale(
                                asset?.trailingDigits ?: 0,
                                RoundingMode.HALF_UP
                        )
                )
        val asset = this.asset ?: return

        valueTextView.text =
                amountFormatter.formatAssetAmount(amount, asset)
        valueHintTextView.text =
                context.getString(R.string.chart_highlight_at_hint,
                        valueHint, DateFormatters.compact(context).format(date))
    }

    private fun displayGrowth(growth: BigDecimal, percent: BigDecimal?) {
        val asset = this.asset ?: return

        if (growth.signum() != 0) {
            val sign = if (growth.signum() < 0) "" else "+"
            val color =
                    if (growth.signum() < 0)
                        ContextCompat.getColor(context, R.color.error)
                    else
                        ContextCompat.getColor(context, R.color.ok)
            var growthString =
                    "$sign${amountFormatter.formatAssetAmount(growth, asset)}"
            if (percent != null) {
                growthString += " ($sign${BigDecimalUtil.toPlainString(percent)}%)"
            }
            growthTextView.textColor = color
            growthTextView.text = growthString
        } else {
            growthTextView.textColor = ContextCompat.getColor(context, R.color.secondary_text)
            growthTextView.setText(R.string.no_growth)
        }

        growthHintTextView.text = context.getString(
                R.string.template_since,
                localizedName.forAssetChartScaleLast(chartScale)
        )
    }

    private val location = IntArray(2)
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        chart.getLocationInWindow(location)
        chartWindowXOffset = location[0].toFloat()
    }
    // endregion

    // region Interaction
    /**
     * Needs to be called if the chart is nested inside a view intercepting touch events
     * i.e. scrollable view.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun applyTouchHook(parent: View) {
        parent.setOnTouchListener { _, event ->
            if (passScrollToChart) {
                val location = IntArray(2)
                parent.getLocationInWindow(location)
                val chartParentXOffset = chartWindowXOffset - location[0].toFloat()

                event.offsetLocation(-chartParentXOffset, 0f)
                chart.dispatchTouchEvent(event)
            }

            false
        }
    }

    /**
     * Progress bar visibility
     */
    var isLoading: Boolean = false
        set(value) {
            field = value
            if (value) {
                progressBar.show()
            } else {
                progressBar.hide()
            }
        }

    /**
     * Label of the main value
     */
    var asset: Asset? = null
        set(value) {
            field = value
            displayTotalValue()
        }

    /**
     * Main total value
     */
    var total: BigDecimal = BigDecimal.ZERO
        set(value) {
            field = value
            displayTotalValue()
        }

    /**
     * Main value hint
     */
    var valueHint: String = ""
        set(value) {
            field = value
            displayTotalValue()
        }

    /**
     * Chart data
     */
    var data: AssetChartData? = null
        set(value) {
            field = value
            updateChart()
        }

    /**
     * Text size of the main value in pixels
     */
    var valueTextSizePx: Float = context.resources.getDimension(R.dimen.text_size_heading_large)
        set(value) {
            field = value
            valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, value)
        }

    val chartView: LineChart
        get() = chart

    /**
     * Sets named horizontal lines that will be displayed over the chart
     */
    fun setLimitLines(limitLines: List<Pair<Float?, String>>) {
        val secondaryColor = ContextCompat.getColor(context, R.color.secondary_text)
        val dash = dip(4).toFloat()
        chart.axisLeft.removeAllLimitLines()
        limitLines
                .asSequence()
                .filter { it.first != null }
                .sortedBy { it.first }
                .toList()
                .forEach {
                    val value = it.first
                    if (value != null) {
                        val limitLine = LimitLine(value, it.second).apply {
                            lineColor = secondaryColor
                            textColor = secondaryColor
                            textSize = 8f
                            labelPosition = LimitLine.LimitLabelPosition.LEFT_BOTTOM
                            enableDashedLine(dash, dash, dash)
                            lineWidth = 0.5f
                        }
                        chart.axisLeft.addLimitLine(limitLine)
                    }
                }
        chart.invalidate()
    }
// endregion
}