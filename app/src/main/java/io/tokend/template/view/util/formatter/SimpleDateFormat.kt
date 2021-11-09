package io.tokend.template.view.util.formatter

import java.text.DateFormat
import java.text.FieldPosition
import java.text.ParsePosition
import java.util.*

class SimpleDateFormat(val formatter: (date: Date) -> String) : DateFormat() {
    override fun parse(source: String, pos: ParsePosition): Date? = null

    override fun format(
        date: Date,
        toAppendTo: StringBuffer,
        fieldPosition: FieldPosition
    ): StringBuffer = toAppendTo.append(formatter(date))
}