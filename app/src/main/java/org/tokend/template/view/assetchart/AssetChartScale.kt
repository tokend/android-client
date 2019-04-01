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
     * @return best looking [DateFormat] for the scale
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
     * @return short label of the scale
     */
    val label: String
        get() = when (this) {
            DAY -> "D"
            WEEK -> "W"
            MONTH -> "M"
            YEAR -> "Y"
        }

    /**
     * @return scale unit name
     */
    val unitName: String
        get() = when (this) {
            DAY -> "day"
            WEEK -> "week"
            MONTH -> "month"
            YEAR -> "year"
        }

    /**
     * @return best looking points count for the scale
     */
    val pointsToDisplay: Int
        get() = when (this) {
            DAY -> 24
            WEEK -> 14
            MONTH -> 31
            YEAR -> 24
        }
}