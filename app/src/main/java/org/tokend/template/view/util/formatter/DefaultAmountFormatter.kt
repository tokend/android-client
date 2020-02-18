package org.tokend.template.view.util.formatter

import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.features.assets.model.Asset
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

class DefaultAmountFormatter : AmountFormatter {

    override fun formatAssetAmount(amount: BigDecimal?,
                                   asset: Asset,
                                   minDecimalDigits: Int,
                                   abbreviation: Boolean,
                                   withAssetCode: Boolean): String {
        val amount = amount ?: BigDecimal.ZERO
        val maxDecimalDigits = asset.trailingDigits

        val formattedAmount = if (abbreviation) {
            val (newAmount, letter) = calculateAmountAbbreviation(amount)
            formatAmount(newAmount, maxDecimalDigits, minDecimalDigits) + letter
        } else {
            formatAmount(amount, maxDecimalDigits, minDecimalDigits)
        }

        return if (withAssetCode) {
            "$formattedAmount\u00A0${asset.code}"
        } else formattedAmount
    }

    override fun formatAmount(amount: BigDecimal?,
                              maxDecimalDigits: Int,
                              minDecimalDigits: Int): String {

        val amount = BigDecimalUtil.stripTrailingZeros((amount ?: BigDecimal.ZERO)
                .setScale(maxDecimalDigits,
                        RoundingMode.DOWN))
        val formatter = buildDecimalFormatter(maxDecimalDigits, minDecimalDigits)
        return formatter.format(amount)
    }

    override fun calculateAmountAbbreviation(amount: BigDecimal): AmountFormatter.Abbreviation {
        return when {
            amount >= BigDecimal(1000000000) ->
                AmountFormatter.Abbreviation(
                        BigDecimalUtil.scaleAmount(
                                amount.divide(BigDecimal(1000000000), MathContext.DECIMAL128),
                                2
                        ),
                        "B"
                )
            amount >= BigDecimal(1000000) ->
                AmountFormatter.Abbreviation(
                        BigDecimalUtil.scaleAmount(
                                amount.divide(BigDecimal(1000000), MathContext.DECIMAL128),
                                2
                        ),
                        "M"
                )
            amount >= BigDecimal(1000) ->
                AmountFormatter.Abbreviation(
                        BigDecimalUtil.scaleAmount(
                                amount.divide(BigDecimal(1000), MathContext.DECIMAL128),
                                2),
                        "K"
                )
            else ->
                AmountFormatter.Abbreviation(amount, "")
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