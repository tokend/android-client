package org.tokend.template.data.model

import org.tokend.sdk.api.trades.model.MatchedOrder
import org.tokend.sdk.utils.BigDecimalUtil
import java.io.Serializable
import java.math.BigDecimal
import java.util.*

class TradeHistoryRecord(
        val pagingToken: String,
        val id: Long,
        val baseAsset: String,
        val quoteAsset: String,
        val baseAmount: BigDecimal,
        val quoteAmount: BigDecimal,
        val price: BigDecimal,
        val createdAt: Date
) : Serializable {

    override fun equals(other: Any?): Boolean {
        return other is TradeHistoryRecord
                && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        @JvmStatic
        fun fromMatchedOrder(source: MatchedOrder): TradeHistoryRecord {
            return TradeHistoryRecord(
                    pagingToken = source.pagingToken,
                    id = source.id.toLong(),
                    baseAsset = source.baseAsset,
                    quoteAsset = source.quoteAsset,
                    baseAmount = BigDecimalUtil.valueOf(source.baseAmount),
                    quoteAmount = BigDecimalUtil.valueOf(source.quoteAmount),
                    price = BigDecimalUtil.valueOf(source.price),
                    createdAt = source.createdAt
            )
        }
    }
}