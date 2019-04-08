package org.tokend.template.data.model

import org.tokend.sdk.api.trades.model.MatchedOrder
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.sdk.utils.HashCodes
import java.io.Serializable
import java.math.BigDecimal
import java.util.*

class TradeHistoryRecord(
        val id: Long,
        val baseAsset: String,
        val quoteAsset: String,
        val baseAmount: BigDecimal,
        val quoteAmount: BigDecimal,
        val price: BigDecimal,
        val createdAt: Date,
        var hasPositiveTrend: Boolean
) : Serializable {

    override fun equals(other: Any?): Boolean {
        return other is TradeHistoryRecord
                && id == other.id
                && hasPositiveTrend == other.hasPositiveTrend
    }

    override fun hashCode(): Int {
        return HashCodes.ofMany(id.hashCode(), hasPositiveTrend.hashCode())
    }

    companion object {
        @JvmStatic
        fun fromMatchedOrder(source: MatchedOrder): TradeHistoryRecord {
            return TradeHistoryRecord(
                    id = source.id.toLong(),
                    baseAsset = source.baseAsset,
                    quoteAsset = source.quoteAsset,
                    baseAmount = BigDecimalUtil.valueOf(source.baseAmount),
                    quoteAmount = BigDecimalUtil.valueOf(source.quoteAmount),
                    price = BigDecimalUtil.valueOf(source.price),
                    createdAt = source.createdAt,
                    hasPositiveTrend = true
            )
        }
    }
}