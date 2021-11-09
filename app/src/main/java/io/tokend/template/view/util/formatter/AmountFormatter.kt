package io.tokend.template.view.util.formatter

import io.tokend.template.features.assets.model.Asset
import java.math.BigDecimal

interface AmountFormatter {

    companion object {
        const val DEFAULT_ASSET_DECIMAL_DIGITS = 6
    }

    /**
     * Formats amount of given asset
     *
     * @param amount amount to format, if set to null then 0 will be used
     * @param asset asset of the amount
     * @param minDecimalDigits minimal count of decimal digits to display
     * @param abbreviation to use or not to use big amount abbreviation i.e. 1100 -> 1.1K
     * @param withAssetCode add or not asset code
     */
    fun formatAssetAmount(
        amount: BigDecimal?,
        asset: Asset,
        minDecimalDigits: Int = 0,
        abbreviation: Boolean = false,
        withAssetCode: Boolean = true
    ): String

    /**
     * Formats given amount
     *
     * @param amount amount to format, if set to null then 0 will be used
     * @param maxDecimalDigits maximal count of decimal digits to display
     * @param minDecimalDigits minimal count of decimal digits to display
     */
    fun formatAmount(
        amount: BigDecimal?,
        maxDecimalDigits: Int,
        minDecimalDigits: Int = 0
    ): String

    data class Abbreviation(val amount: BigDecimal, val letter: String)

    fun calculateAmountAbbreviation(amount: BigDecimal): Abbreviation
}