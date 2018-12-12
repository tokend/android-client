package org.tokend.template.view.assetchart

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

enum class AssetChartScale {
    DAY,
    WEEK,
    MONTH,
    YEAR;

    /**
     * @returns best looking [DateFormat] for the scale
     */
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

    /**
     * @returns short label of the scale
     */
    val label: String
        get() = when (this) {
            DAY -> "D"
            WEEK -> "W"
            MONTH -> "M"
            YEAR -> "Y"
        }

    /**
     * @returns scale unit name
     */
    val unitName: String
        get() = when (this) {
            DAY -> "day"
            WEEK -> "week"
            MONTH -> "month"
            YEAR -> "year"
        }
}