package org.tokend.template.data.model

import org.tokend.sdk.api.trades.model.MatchedOrder
import java.io.Serializable
import java.util.*

class TradeHistoryRecord(
        val pagingToken: String,
        val id: Long,
        val baseAsset: String,
        val quoteAsset: String,
        val baseAmount: String,
        val quoteAmount: String,
        val price: String,
        val createdAt: Date
) : Serializable {

    companion object {
        @JvmStatic
        fun fromMatchedOrder(source: MatchedOrder): TradeHistoryRecord {
            return TradeHistoryRecord(
                    pagingToken = source.pagingToken,
                    id = source.id.toLong(),
                    baseAsset = source.baseAsset,
                    quoteAsset = source.quoteAsset,
                    baseAmount = source.baseAmount,
                    quoteAmount = source.quoteAmount,
                    price = source.price,
                    createdAt = source.createdAt
            )
        }
    }
}