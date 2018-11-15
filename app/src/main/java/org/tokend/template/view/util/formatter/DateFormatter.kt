package org.tokend.template.view.util.formatter

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

class DateFormatter(private val context: Context) {
    fun formatLong(date: Date): String {
        val timeFormat = android.text.format.DateFormat.getTimeFormat(context)
        val formattedTime = timeFormat.format(date)
        val formattedDate = formatDateOnly(date)

        return "$formattedDate $formattedTime"
    }

    fun formatCompact(date: Date): String {
        return SimpleDateFormat("HH:mm dd MMM yy", Locale.ENGLISH)
                .format(date)
    }

    fun formatDateOnly(date: Date): String {
        return SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
                .format(date)
    }
}