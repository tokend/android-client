package org.tokend.template.features.limits

import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.text.SpannableString
import android.view.View
import kotlinx.android.synthetic.main.layout_card_limit.view.*
import kotlinx.android.synthetic.main.layout_limit_progress.view.*
import org.tokend.sdk.api.accounts.model.limits.LimitEntry
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.R
import org.tokend.template.extensions.highlight
import org.tokend.template.extensions.isMaxPossibleAmount
import org.tokend.template.view.util.formatter.AmountFormatter
import java.math.BigDecimal

class LimitProgressWrapper(private val rootView: View,
                           private val amountFormatter: AmountFormatter) {

    private val context = rootView.context
    private val highlightColor = ContextCompat.getColor(context, R.color.accent)

    fun displayProgress(entry: LimitEntry, asset: String) {
        setProgress(rootView.daily_limit, entry.statistics.daily,
                entry.limit.daily, asset, R.string.limit_period_daily)

        setProgress(rootView.weekly_limit, entry.statistics.weekly,
                entry.limit.weekly, asset, R.string.limit_period_weekly)

        setProgress(rootView.monthly_limit, entry.statistics.monthly,
                entry.limit.monthly, asset, R.string.limit_period_monthly)

        setProgress(rootView.annual_limit, entry.statistics.annual,
                entry.limit.annual, asset, R.string.limit_period_annual)
    }

    private fun setProgress(progressLayout: View,
                            used: BigDecimal,
                            total: BigDecimal,
                            asset: String,
                            @StringRes
                            period: Int) {

        if(total.isMaxPossibleAmount()) {
            progressLayout.visibility = View.GONE
            return
        }

        progressLayout.limit_period.text = context.getString(period)

        val leftTextView = progressLayout.limit_left
        val totalTextView = progressLayout.limit_total

        val left = amountFormatter.formatAssetAmount(total - used, asset,
                abbreviation = true, withAssetCode = true)
        val leftString = context.getString(R.string.template_limit_left, left)
        leftTextView.text = SpannableString(leftString)
                .apply { highlight(left, highlightColor) }

        val max = amountFormatter.formatAssetAmount(total, asset,
                abbreviation = true, withAssetCode = true)
        val maxString = context.getString(R.string.template_limit_total, max)
        totalTextView.text = SpannableString(maxString)
                .apply { highlight(max, highlightColor) }

        val progress = progressLayout.limit_progress
        val scaledTotalCap = BigDecimalUtil.scaleAmount(total, 0).toInt()
        val scaledLeftCap = BigDecimalUtil.scaleAmount(total - used, 0).toInt()
        progress.max = scaledTotalCap
        progress.progress = scaledLeftCap
    }
}