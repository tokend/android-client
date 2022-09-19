package io.tokend.template.features.invest.view

import android.text.SpannableString
import android.view.View
import androidx.core.content.ContextCompat
import io.tokend.template.R
import io.tokend.template.extensions.highlight
import io.tokend.template.features.invest.model.SaleRecord
import io.tokend.template.view.util.LocalizedName
import io.tokend.template.view.util.RemainedTimeUtil
import io.tokend.template.view.util.formatter.AmountFormatter
import kotlinx.android.synthetic.main.layout_sale_progress.view.*
import org.tokend.sdk.utils.BigDecimalUtil
import kotlin.math.roundToInt

class SaleProgressWrapper(
    private val rootView: View,
    private val amountFormatter: AmountFormatter
) {
    private val localizedName = LocalizedName(rootView.context)

    fun displayProgress(sale: SaleRecord) {
        val context = rootView.context
        val highlightColor = ContextCompat.getColor(context, R.color.accent)

        val investedAmountString = amountFormatter.formatAssetAmount(
            sale.currentCap,
            sale.defaultQuoteAsset, abbreviation = true
        )
        val investedString =
            context.getString(R.string.template_sale_invested, investedAmountString)

        val investedSpannableString = SpannableString(investedString)
        investedSpannableString.highlight(investedAmountString, highlightColor)
        rootView.sale_invested_text_view.text = investedSpannableString

        val scaledCurrentCap = BigDecimalUtil.scaleAmount(
            sale.currentCap,
            0
        ).toInt()
        val scaledSoftCap = BigDecimalUtil.scaleAmount(
            sale.softCap,
            0
        ).toInt()

        rootView.sale_progress.max = scaledSoftCap
        rootView.sale_progress.progress = scaledCurrentCap

        val percent = "${(scaledCurrentCap * 100f / scaledSoftCap).roundToInt()}%"
        val funded = context.getString(R.string.template_sale_funded, percent)
        rootView.sale_progress_percent_text_view.text = SpannableString(funded)
            .apply { highlight(percent, highlightColor) }

        when {
            sale.isUpcoming || sale.isAvailableForInvestment -> {
                rootView.sale_remain_time_text_view.visibility = View.VISIBLE

                val date =
                    if (sale.isAvailableForInvestment)
                        sale.endDate
                    else
                        sale.startDate

                val (timeValue, timeUnit) = RemainedTimeUtil.getRemainedTime(date)

                val templateRes =
                    if (sale.isAvailableForInvestment)
                        R.string.template_sale_days_to_go
                    else
                        R.string.template_sale_starts_in

                val daysString =
                    context.getString(
                        templateRes,
                        timeValue,
                        localizedName.forTimeUnit(timeUnit, timeValue)
                    )
                val toHighlight = daysString.substringBefore('\n')

                rootView.sale_remain_time_text_view.text = SpannableString(daysString)
                    .apply { highlight(toHighlight, highlightColor) }
            }
            sale.isCanceled -> {
                rootView.sale_remain_time_text_view.apply {
                    visibility = View.VISIBLE
                    text = context.getString(R.string.sale_cancelled)
                }
            }
            sale.isClosed -> {
                rootView.sale_remain_time_text_view.apply {
                    visibility = View.VISIBLE
                    text = context.getString(R.string.sale_closed)
                }
            }
            sale.isEnded -> {
                rootView.sale_remain_time_text_view.apply {
                    visibility = View.VISIBLE
                    text = context.getString(R.string.sale_ended)
                }
            }
            else -> {
                rootView.sale_remain_time_text_view.visibility = View.GONE
            }
        }
    }
}