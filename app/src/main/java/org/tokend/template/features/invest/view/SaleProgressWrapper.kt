package org.tokend.template.features.invest.view

import android.support.v4.content.ContextCompat
import android.text.SpannableString
import android.view.View
import kotlinx.android.synthetic.main.layout_sale_progress.view.*
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.R
import org.tokend.template.extensions.highlight
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.view.util.LocalizedName
import org.tokend.template.view.util.RemainedTimeUtil
import org.tokend.template.view.util.formatter.AmountFormatter
import kotlin.math.roundToInt

class SaleProgressWrapper(private val rootView: View,
                          private val amountFormatter: AmountFormatter
) {
    private val localizedName = LocalizedName(rootView.context)

    fun displayProgress(sale: SaleRecord) {
        val context = rootView.context
        val highlightColor = ContextCompat.getColor(context, R.color.accent)

        val investedAmountString = amountFormatter.formatAssetAmount(sale.currentCap,
                sale.defaultQuoteAsset, abbreviation = true)
        val investedString = context.getString(R.string.template_sale_invested, investedAmountString)

        val investedSpannableString = SpannableString(investedString)
        investedSpannableString.highlight(investedAmountString, highlightColor)
        rootView.sale_invested_text_view.text = investedSpannableString

        val scaledCurrentCap = BigDecimalUtil.scaleAmount(sale.currentCap,
                0).toInt()
        val scaledSoftCap = BigDecimalUtil.scaleAmount(sale.softCap,
                0).toInt()

        rootView.sale_progress.max = scaledSoftCap
        rootView.sale_progress.progress = scaledCurrentCap

        val percent = "${(scaledCurrentCap * 100f / scaledSoftCap).roundToInt()}%"
        val funded = context.getString(R.string.template_sale_funded, percent)
        rootView.sale_progress_percent_text_view.text = SpannableString(funded)
                .apply { highlight(percent, highlightColor) }

        if (sale.isAvailable || sale.isUpcoming) {
            rootView.sale_remain_time_text_view.visibility = View.VISIBLE

            val date =
                    if (sale.isAvailable)
                        sale.endDate
                    else
                        sale.startDate

            val (timeValue, timeUnit) = RemainedTimeUtil.getRemainedTime(date)

            val templateRes =
                    if (sale.isAvailable)
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
        } else {
            rootView.sale_remain_time_text_view.visibility = View.GONE
        }
    }
}