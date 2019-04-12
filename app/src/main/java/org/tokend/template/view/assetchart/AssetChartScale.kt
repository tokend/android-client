package org.tokend.template.view.assetchart

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

enum class AssetChartScale {
    HOUR,
    DAY,
    MONTH,
    YEAR;

    /**
     * @return best looking [DateFormat] for the scale
     */
    val dateFormat: DateFormat
        get() {
            val formatString = when (this) {
                HOUR, DAY -> "HH:mm"
                MONTH -> "dd MMM"
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
            HOUR -> "H"
            DAY -> "D"
            MONTH -> "M"
            YEAR -> "Y"
        }

    /**
     * @return scale unit name
     */
    val unitName: String
        get() = when (this) {
            HOUR -> "hour"
            DAY -> "day"
            MONTH -> "month"
            YEAR -> "year"
        }

    /**
     * @return best looking points count for the scale
     */
    val pointsToDisplay: Int
        get() = when (this) {
            HOUR -> 30
            DAY -> 24
            MONTH -> 31
            YEAR -> 24
        }
}