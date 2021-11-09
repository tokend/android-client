package io.tokend.template.features.trade.pairs.logic

import java.math.BigDecimal

interface AmountConverter {
    /**
     * @return multiplier for conversion from source asset to dest one.
     * If there is no such pair [null] will be returned.
     */
    fun getRate(sourceAsset: String, destAsset: String): BigDecimal?

    /**
     * @return multiplier for conversion from source asset to dest one.
     * If there is no such pair "1" will be returned.
     */
    fun getRateOrOne(sourceAsset: String, destAsset: String): BigDecimal {
        return getRate(sourceAsset, destAsset) ?: BigDecimal.ONE
    }

    /**
     * @return amount converted from source asset to dest one using found rate
     * or [null] if no rate found.
     * @see AmountConverter.getRate
     */
    fun convert(amount: BigDecimal, sourceAsset: String, destAsset: String): BigDecimal?

    /**
     * @return amount converted from source asset to dest one using found rate
     * or same one if no rate found.
     * @see AmountConverter.getRateOrOne
     */
    fun convertOrSame(amount: BigDecimal, sourceAsset: String, destAsset: String): BigDecimal {
        return convert(amount, sourceAsset, destAsset) ?: amount
    }
}