package io.tokend.template.view.util.formatter

import android.content.Context
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


object DateFormatters {
    // Expecting it was set by locale manager.
    private val locale: Locale
        get() = Locale.getDefault()

    /**
     * Formats given date to the long string with date only
     */
    private val longDateOnly: DateFormat
        get() = SimpleDateFormat("dd MMMM yyyy", locale)

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
            compactDateOnly(includeYear = true).format(date) + " " + longTimeOnly(context).format(
                date
            )
        }
    }

    /**
     * Formats given date to 12-/24-hour time if it is today.
     * Otherwise formats to date with short month name including 2-digits year number
     * if the year is different from the current one.
     */
    fun timeOrDate(context: Context): DateFormat {
        return SimpleDateFormat { date ->
            val currentCalendar = Calendar.getInstance()
            val currentDay = currentCalendar.get(Calendar.DAY_OF_YEAR)
            val currentYear = currentCalendar.get(Calendar.YEAR)

            val actionCalendar = Calendar.getInstance().apply { timeInMillis = date.time }
            val actionDay = actionCalendar.get(Calendar.DAY_OF_YEAR)
            val actionYear = actionCalendar.get(Calendar.YEAR)

            when {
                currentDay == actionDay && currentYear == actionYear ->
                    longTimeOnly(context)
                else ->
                    compactDateOnly(includeYear = currentYear != actionYear)
            }.format(date)
        }
    }

    /**
     * Formats given date to the long string without time:
     * short month name, 2-digits year number
     */
    private fun compactDateOnly(includeYear: Boolean = true): DateFormat {
        return if (includeYear)
            SimpleDateFormat("dd MMM yy", locale)
        else
            SimpleDateFormat("dd MMM", locale)
    }
}


