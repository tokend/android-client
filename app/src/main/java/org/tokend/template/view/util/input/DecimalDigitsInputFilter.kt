package org.tokend.template.view.util.input

import android.text.InputFilter
import android.text.Spanned
import android.text.TextUtils
import java.util.regex.Pattern

/**
 * Limits numeric edit text input by count of digits before and after coma
 */
class DecimalDigitsInputFilter(digitsBeforeComa: Int?, digitsAfterComa: Int?) : InputFilter {
    private val mDigitsBeforeZero: Int
    private val mDigitsAfterZero: Int
    private var mPattern: Pattern

    init {
        this.mDigitsBeforeZero = digitsBeforeComa ?: DIGITS_BEFORE_COMA_DEFAULT
        this.mDigitsAfterZero = digitsAfterComa ?: DIGITS_AFTER_COMA_DEFAULT
        mPattern = Pattern.compile("-?[0-9]{0," + mDigitsBeforeZero + "}+((\\.[0-9]{0," + mDigitsAfterZero
                + "})?)||(\\.)?")
    }

    override fun filter(source: CharSequence, start: Int, end: Int,
                        dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        val replacement = source.subSequence(start, end).toString()
        val newVal = dest.subSequence(0, dstart).toString() + replacement +
                dest.subSequence(dend, dest.length).toString()
        val matcher = mPattern.matcher(newVal)
        if (matcher.matches())
            return null

        return if (TextUtils.isEmpty(source))
            dest.subSequence(dstart, dend)
        else
            ""
    }

    private companion object {
        private const val DIGITS_BEFORE_COMA_DEFAULT = 100
        private const val DIGITS_AFTER_COMA_DEFAULT = 100
    }
}