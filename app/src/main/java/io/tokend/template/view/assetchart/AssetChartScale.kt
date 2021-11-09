package io.tokend.template.view.assetchart

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
     * @return best looking points count for the scale
     */
    val pointsToDisplay: Int
        get() = when (this) {
            HOUR -> 60
            DAY -> 72
            MONTH -> 93
            YEAR -> 120
        }
}