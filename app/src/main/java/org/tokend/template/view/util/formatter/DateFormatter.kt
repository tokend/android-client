package org.tokend.template.view.util.formatter

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

class DateFormatter(private val context: Context) {
    // Expecting it was set by locale manager.
    private val locale = Locale.getDefault()

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
        return SimpleDateFormat("dd MMMM yyyy", locale)
                .format(date)
    }

    /**
     * Formats given date to the long string without time:
     * short month name, 2-digits year number
     */
    fun formatCompactDateOnly(date: Date,
                              includeYear: Boolean = true): String {
        val pattern =
                if (includeYear)
                    "dd MMM yy"
                else
                    "dd MMM"

        return SimpleDateFormat(pattern, locale)
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
     * Otherwise formats to date with short month name including 2-digits year number
     * if the year is different from the current one.
     */
    fun formatTimeOrDate(date: Date): String {
        val currentCalendar = Calendar.getInstance()
        val currentDay = currentCalendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = currentCalendar.get(Calendar.YEAR)

        val actionCalendar = Calendar.getInstance().apply { timeInMillis = date.time }
        val actionDay = actionCalendar.get(Calendar.DAY_OF_YEAR)
        val actionYear = actionCalendar.get(Calendar.YEAR)

        return when {
            currentDay == actionDay
                    && currentYear == actionYear -> formatTimeOnly(date)
            else -> formatCompactDateOnly(date, includeYear = currentYear != actionYear)
        }
    }
}