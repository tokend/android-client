package org.tokend.template.view.util.input

import android.text.Editable
import android.widget.EditText
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.view.util.formatter.AmountFormatter
import java.math.BigDecimal
import java.math.RoundingMode

typealias AmountChangeListener = (scaled: BigDecimal, raw: BigDecimal) -> Unit

class AmountEditTextWrapper(private val editText: EditText) {
    private var amountListener: AmountChangeListener? = null
    private var textWatcher = object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable?) {
            updateAmount(s?.toString() ?: "")
        }
    }

    fun onAmountChanged(listener: AmountChangeListener) {
        this.amountListener = listener
    }

    var maxPlacesBeforeComa = 8
        set(value) {
            field = value
            trimInputAndUpdateAmount()
        }
    var maxPlacesAfterComa = AmountFormatter.ASSET_DECIMAL_DIGITS
        set(value) {
            field = value
            trimInputAndUpdateAmount()
        }

    var rawAmount: BigDecimal = BigDecimal.ZERO
        private set
    val scaledAmount: BigDecimal
        get() = BigDecimalUtil.scaleAmount(rawAmount, maxPlacesAfterComa)

    init {
        editText.removeTextChangedListener(textWatcher)
        editText.addTextChangedListener(textWatcher)
        updateInputFilter()
    }

    private fun updateAmount(input: String) {
        rawAmount = BigDecimalUtil.valueOf(input)
        amountListener?.invoke(scaledAmount, rawAmount)
    }

    private fun trimInput(input: String): String {
        if (input.isEmpty()) {
            return input
        }

        if (input == ".") {
            return "0.0"
        }

        val currentAmount = BigDecimalUtil.valueOf(input)
                .setScale(maxPlacesAfterComa, RoundingMode.DOWN)

        return BigDecimalUtil.stripTrailingZeros(currentAmount).toPlainString()
    }

    private fun trimInputAndUpdateAmount() {
        updateInputFilter()
        editText.setText(trimInput(editText.text.toString()))
        editText.setSelection(editText.text.length)
    }

    private fun updateInputFilter() {
        editText.filters = arrayOf(DecimalDigitsInputFilter(maxPlacesBeforeComa, maxPlacesAfterComa))
    }
}