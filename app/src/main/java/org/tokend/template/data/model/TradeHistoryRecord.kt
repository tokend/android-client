package org.tokend.template.data.model

import org.tokend.sdk.api.trades.model.MatchedOrder
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.sdk.utils.HashCodes
import org.tokend.template.data.repository.base.pagination.PagingRecord
import java.io.Serializable
import java.math.BigDecimal
import java.util.*

class TradeHistoryRecord(
        val id: Long,
        val baseAsset: Asset,
        val quoteAsset: Asset,
        val baseAmount: BigDecimal,
        val quoteAmount: BigDecimal,
        val price: BigDecimal,
        val createdAt: Date,
        var hasPositiveTrend: Boolean
) : Serializable, PagingRecord {
    override fun getPagingId(): Long = id

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
                    baseAsset = SimpleAsset(source.baseAsset),
                    quoteAsset = SimpleAsset(source.quoteAsset),
                    baseAmount = BigDecimalUtil.valueOf(source.baseAmount),
                    quoteAmount = BigDecimalUtil.valueOf(source.quoteAmount),
                    price = BigDecimalUtil.valueOf(source.price),
                    createdAt = source.createdAt,
                    hasPositiveTrend = true
            )
        }
    }
}