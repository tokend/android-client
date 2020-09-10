package org.tokend.template.view.util.formatter

import android.content.Context
import java.util.*

interface DateFormatter{
    fun format(date: Date, context: Context?): String
}