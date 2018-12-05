package org.tokend.template.extensions

import android.support.annotation.ColorInt
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan

/**
 * Highlights first occurrence of given substring with given color
 */
fun SpannableString.highlight(toHighlight: String, @ColorInt color: Int) {
    val start = indexOf(toHighlight)
    val end = start + toHighlight.length
    setSpan(ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}