package org.tokend.template.view.util.formatter

import org.tokend.sdk.utils.BigDecimalUtil
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

object AmountFormatter {
    const val ASSET_DECIMAL_DIGITS = 6
    const val FIAT_DECIMAL_DIGITS = 2

    /**
     * Formats amount of given asset
     *
     * @param amount amount to format, if set to null then 0 will be used
     * @param asset asset of the amount
     * @param minDecimalDigits minimal count of decimal digits to display
     * @param abbreviation to use or not to use big amount abbreviation i.e. 1100 -> 1.1K
     */
    fun formatAssetAmount(amount: BigDecimal?,
                          asset: String? = "",
                          minDecimalDigits: Int = 0,
                          abbreviation: Boolean = false): String {
        val amount = amount ?: BigDecimal.ZERO

        return if (abbreviation) {
            val (newAmount, letter) = calculateAmountAbbreviation(amount)
            formatAmount(newAmount, getDecimalDigitsCount(asset), minDecimalDigits) + letter
        } else {
            formatAmount(amount, getDecimalDigitsCount(asset), minDecimalDigits)
        }
    }

    /**
     * Formats amount of given asset
     *
     * @see formatAssetAmount
     */
    fun formatAssetAmount(amount: String?,
                          asset: String? = "",
                          minDecimalDigits: Int = 0,
                          abbreviation: Boolean = false): String {
        val amount = if (amount.isNullOrBlank()) "0" else amount
        return formatAssetAmount(BigDecimal(amount), asset, minDecimalDigits, abbreviation)
    }

    /**
     * Formats given amount
     *
     * @param amount amount to format, if set to null then 0 will be used
     * @param maxDecimalDigits maximal count of decimal digits to display
     * @param minDecimalDigits minimal count of decimal digits to display
     */
    fun formatAmount(amount: BigDecimal?, maxDecimalDigits: Int,
                     minDecimalDigits: Int = 0): String {
        val amount = BigDecimalUtil.stripTrailingZeros((amount ?: BigDecimal.ZERO)
                .setScale(maxDecimalDigits,
                        RoundingMode.DOWN))
        val formatter = buildDecimalFormatter(maxDecimalDigits, minDecimalDigits)
        return formatter.format(amount)
    }

    private data class Abbreviation(val amount: BigDecimal, val letter: String)

    private fun calculateAmountAbbreviation(amount: BigDecimal): Abbreviation {
        return when {
            amount >= BigDecimal(1000000000) ->
                Abbreviation(amount.divide(BigDecimal(1000000000), MathContext.DECIMAL128), "G")
            amount >= BigDecimal(1000000) ->
                Abbreviation(amount.divide(BigDecimal(1000000), MathContext.DECIMAL128), "M")
            amount >= BigDecimal(1000) ->
                Abbreviation(amount.divide(BigDecimal(1000), MathContext.DECIMAL128), "K")
            else ->
                Abbreviation(amount, "")
        }
    }

    /**
     * @return maximal decimal digits count for the asset
     */
    fun getDecimalDigitsCount(asset: String?): Int {
        return when (asset) {
            "USD", "EUR" -> FIAT_DECIMAL_DIGITS
            else -> ASSET_DECIMAL_DIGITS
        }
    }

    private fun buildDecimalFormatter(maxZerosCount: Int, minZerosCount: Int): NumberFormat {
        val symbols = DecimalFormatSymbols()
        symbols.decimalSeparator = '.'
        symbols.groupingSeparator = ','
        val df = DecimalFormat()
        df.roundingMode = RoundingMode.DOWN
        df.decimalFormatSymbols = symbols
        df.maximumFractionDigits = maxZerosCount
        df.minimumFractionDigits = minZerosCount
        df.isGroupingUsed = true
        return df
    }
}