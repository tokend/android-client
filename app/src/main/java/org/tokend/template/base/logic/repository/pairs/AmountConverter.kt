package org.tokend.template.base.logic.repository.pairs

import java.math.BigDecimal

interface AmountConverter {
    /**
     * @return multiplier for conversion from source asset to dest one.
     * If there is no such pair "1" will be returned.
     */
    fun getRate(sourceAsset: String, destAsset: String): BigDecimal

    /**
     * @return amount converted from source asset to dest one using found rate.
     * @see AmountConverter.getRate
     */
    fun convertAmount(amount: BigDecimal?, sourceAsset: String, destAsset: String): BigDecimal
}