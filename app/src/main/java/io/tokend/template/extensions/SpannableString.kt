package io.tokend.template.extensions

import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt

/**
 * Highlights first occurrence of given substring with given color
 */
fun SpannableString.highlight(toHighlight: String, @ColorInt color: Int) {
    val start = indexOf(toHighlight)
    val end = start + toHighlight.length
    setSpan(ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}

/**
 * Highlights last occurrence of given substring with given color
 */
fun SpannableString.highlightLast(toHighlight: String, @ColorInt color: Int) {
    val start = lastIndexOf(toHighlight)
    val end = start + toHighlight.length
    setSpan(ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}

/**
 * Changes font size of the first occurrence of given substring
 */
fun SpannableString.setFontSize(substring: String, fontSizePx: Int) {
    val start = indexOf(substring)
    val end = start + substring.length
    setSpan(AbsoluteSizeSpan(fontSizePx), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}