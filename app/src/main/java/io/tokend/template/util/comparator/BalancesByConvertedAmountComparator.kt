package io.tokend.template.util.comparator

import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.balances.model.BalanceRecord
import java.math.BigDecimal

/**
 * Compares [BalanceRecord]s by converted amount.
 * Does not perform [BalanceRecord.conversionAssetCode] checks.
 * Order is descending
 *
 * @param fallbackAssetComparator comparator for asset codes, will be used
 * for balances with equal converted amount
 */
class BalancesByConvertedAmountComparator(
    private val fallbackAssetComparator: Comparator<Asset>?
) : Comparator<BalanceRecord> {
    override fun compare(o1: BalanceRecord, o2: BalanceRecord): Int {
        val converted1 = o1.convertedAmount ?: BigDecimal.ZERO
        val converted2 = o2.convertedAmount ?: BigDecimal.ZERO

        val result = converted2.compareTo(converted1)
        return if (result == 0 && fallbackAssetComparator != null)
            fallbackAssetComparator.compare(o1.asset, o2.asset)
        else
            result
    }
}