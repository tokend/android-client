package org.tokend.template.base.view.asset_chart

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Oleg Koretsky on 1/8/18.
 */
enum class AssetChartScale {
    DAY,
    WEEK,
    MONTH,
    YEAR;

    val dateFormat: DateFormat
        get() {
            val formatString = when (this) {
                DAY -> "HH:mm"
                WEEK, MONTH -> "dd MMM"
                YEAR -> "MMM yyyy"
            }
            val format = SimpleDateFormat(formatString, Locale.ENGLISH)
            format.timeZone = TimeZone.getDefault()
            return format
        }

    val label: String
        get() = when (this) {
            DAY -> "D"
            WEEK -> "W"
            MONTH -> "M"
            YEAR -> "Y"
        }

    val unitName: String
        get() = when (this) {
            DAY -> "day"
            WEEK -> "week"
            MONTH -> "month"
            YEAR -> "year"
        }
}