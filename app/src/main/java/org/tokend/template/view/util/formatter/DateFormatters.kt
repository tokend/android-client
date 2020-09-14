package org.tokend.template.view.util.formatter

import android.content.Context
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


object DateFormatters {
    // Expecting it was set by locale manager.
    private var locale = Locale.getDefault()

    /**
     * Formats given date to the long string with date only
     */
    private val longDateOnly: DateFormat by lazy {
        SimpleDateFormat("dd MMMM yyyy", locale)
    }

    /**
     * Formats given date to the long string with time only:
     * 12-/24-hour time based on device preference
     */
    private fun longTimeOnly(context: Context): DateFormat {
        return android.text.format.DateFormat.getTimeFormat(context)
    }

    /**
     * Formats given date to the long string:
     * full month name, full year number, 12-/24-hour time based on device preference
     */
    fun long(context: Context): DateFormat {
        return SimpleDateFormat { date ->
            longDateOnly.format(date) + " " + longTimeOnly(context).format(date)
        }
    }

    /**
     * Formats given date to the compact string:
     * short month name, 2-digits year number, 12-/24-hour time based on device preference
     */
    fun compact(context: Context): DateFormat {
        return SimpleDateFormat { date ->
            formatCompactDateOnly(date) + " " + longTimeOnly(context)
        }
    }

    /**
     * Formats given date to 12-/24-hour time if it is today.
     * Otherwise formats to date with short month name including 2-digits year number
     * if the year is different from the current one.
     */
    fun timeOrDate(date: Date, context: Context): DateFormat {
        val currentCalendar = Calendar.getInstance()
        val currentDay = currentCalendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = currentCalendar.get(Calendar.YEAR)

        val actionCalendar = Calendar.getInstance().apply { timeInMillis = date.time }
        val actionDay = actionCalendar.get(Calendar.DAY_OF_YEAR)
        val actionYear = actionCalendar.get(Calendar.YEAR)

        return when {
            currentDay == actionDay
                    && currentYear == actionYear -> longTimeOnly(context)
            else -> SimpleDateFormat {
                formatCompactDateOnly(it, includeYear = currentYear != actionYear)
            }
        }
    }

    /**
     * Formats given date to the long string without time:
     * short month name, 2-digits year number
     */
    private fun formatCompactDateOnly(date: Date, includeYear: Boolean = true): String {
        val pattern =
                if (includeYear)
                    "dd MMM yy"
                else
                    "dd MMM"

        return SimpleDateFormat(pattern, locale)
                .format(date)
    }
}


