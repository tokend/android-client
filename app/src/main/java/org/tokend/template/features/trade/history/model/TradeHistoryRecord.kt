package org.tokend.template.features.trade.history.model

import org.tokend.sdk.api.trades.model.MatchedOrder
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.data.repository.base.pagination.PagingRecord
import org.tokend.template.features.assets.model.Asset
import org.tokend.template.features.assets.model.SimpleAsset
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
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TradeHistoryRecord

        if (id != other.id) return false
        if (hasPositiveTrend != other.hasPositiveTrend) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + hasPositiveTrend.hashCode()
        return result
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