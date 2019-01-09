package org.tokend.template.view.util.formatter

import java.math.BigDecimal

interface AmountFormatter {

    companion object {
        const val DEFAULT_ASSET_DECIMAL_DIGITS = 6
        const val DEFAULT_FIAT_DECIMAL_DIGITS = 2
    }

    /**
     * Formats amount of given asset
     *
     * @param amount amount to format, if set to null then 0 will be used
     * @param asset asset of the amount
     * @param minDecimalDigits minimal count of decimal digits to display
     * @param abbreviation to use or not to use big amount abbreviation i.e. 1100 -> 1.1K
     */
    fun formatAssetAmount(amount: BigDecimal?,
                          asset: String,
                          minDecimalDigits: Int = 0,
                          abbreviation: Boolean = false): String

    /**
     * Formats amount of given asset
     *
     * @see formatAssetAmount
     */
    fun formatAssetAmount(amount: String?,
                          asset: String,
                          minDecimalDigits: Int = 0,
                          abbreviation: Boolean = false): String

    /**
     * Formats given amount
     *
     * @param amount amount to format, if set to null then 0 will be used
     * @param maxDecimalDigits maximal count of decimal digits to display
     * @param minDecimalDigits minimal count of decimal digits to display
     */
    fun formatAmount(amount: BigDecimal?,
                     maxDecimalDigits: Int,
                     minDecimalDigits: Int = 0): String

    /**
     * @return maximal decimal digits count for the asset
     */
    fun getDecimalDigitsCount(asset: String?): Int

    data class Abbreviation(val amount: BigDecimal, val letter: String)

    fun calculateAmountAbbreviation(amount: BigDecimal): Abbreviation
}