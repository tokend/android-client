package org.tokend.template.view.util.formatter

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

class DateFormatter(private val context: Context) {
    /**
     * Formats given date to the long string:
     * full month name, full year number, 12-/24-hour time based on device preference
     */
    fun formatLong(date: Date): String {
        return "${formatDateOnly(date)} ${formatTimeOnly(date)}"
    }

    /**
     * Formats given date to the compact string:
     * short month name, 2-digits year number, 12-/24-hour time based on device preference
     */
    fun formatCompact(date: Date): String {
        return "${formatCompactDateOnly(date)} ${formatTimeOnly(date)}"
    }

    /**
     * Formats given date to the long string without time:
     * full month name, full year number
     */
    fun formatDateOnly(date: Date): String {
        return SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
                .format(date)
    }

    /**
     * Formats given date to the long string without time:
     * short month name, 2-digits year number
     */
    fun formatCompactDateOnly(date: Date): String {
        return SimpleDateFormat("dd MMM yy", Locale.ENGLISH)
                .format(date)
    }

    /**
     * Formats given date to the long string with time only:
     * 12-/24-hour time based on device preference
     */
    fun formatTimeOnly(date: Date): String {
        val timeFormat = android.text.format.DateFormat.getTimeFormat(context)
        return timeFormat.format(date)
    }

    /**
     * Formats given date to 12-/24-hour time if it is today.
     * Otherwise formats to date with short month name, 2-digits year number.
     */
    fun formatTimeOrDate(date: Date): String {
        val current = Calendar.getInstance().get(Calendar.DATE)
        val action = Calendar.getInstance().apply { timeInMillis = date.time }.get(Calendar.DATE)

        return when (current) {
            action -> {
                formatTimeOnly(date)
            }
            else -> formatCompactDateOnly(date)
        }
    }
}