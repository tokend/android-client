package io.tokend.template.view.util.input

import android.widget.EditText
import io.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.sdk.utils.BigDecimalUtil
import java.math.BigDecimal
import java.math.RoundingMode

typealias AmountChangeListener = (scaled: BigDecimal, raw: BigDecimal) -> Unit

/**
 * Wraps [EditText] to simplify amount input
 */
class AmountEditTextWrapper(
    private val editText: EditText,
    maxLength: Boolean = false,
) {
    private var amountListener: AmountChangeListener? = null
    private var textWatcher = SimpleTextWatcher {
        updateAmount(it?.toString() ?: "")
    }

    /**
     * Sets amount change listener
     */
    fun onAmountChanged(listener: AmountChangeListener) {
        this.amountListener = listener
    }

    /**
     * Limit for decimal digits before coma
     */
    var maxPlacesBeforeComa = if (maxLength) MAX_LENGTH else 8
        set(value) {
            field = value
            trimInputAndUpdateAmount()
        }

    /**
     * Limit for decimal digits after coma
     */
    var maxPlacesAfterComa = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS
        set(value) {
            field = value
            trimInputAndUpdateAmount()
        }

    /**
     * Unscaled raw parsed amount
     */
    var rawAmount: BigDecimal = BigDecimal.ZERO
        private set

    /**
     * Scaled parsed amount
     *
     * @see maxPlacesAfterComa
     */
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
        editText.filters =
            arrayOf(DecimalDigitsInputFilter(maxPlacesBeforeComa, maxPlacesAfterComa))
    }

    companion object {
        const val MAX_LENGTH = 100
    }
}