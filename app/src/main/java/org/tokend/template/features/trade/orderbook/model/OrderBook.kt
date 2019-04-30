package org.tokend.template.features.trade.orderbook.model

import org.tokend.sdk.api.generated.resources.OrderBookResource
import java.math.BigDecimal

class OrderBook(
        val id: String,
        val buyEntries: List<OrderBookEntryRecord>,
        val sellEntries: List<OrderBookEntryRecord>,
        val baseAssetCode: String,
        val quoteAssetCode: String
) {
    val maxBuyVolume: BigDecimal = buyEntries.lastOrNull()?.volume ?: BigDecimal.ZERO

    val maxSellVolume: BigDecimal = sellEntries.lastOrNull()?.volume ?: BigDecimal.ZERO

    constructor(source: OrderBookResource) : this(
            id = source.id,
            buyEntries = source.buyEntries.map(::OrderBookEntryRecord),
            sellEntries = source.sellEntries.map(::OrderBookEntryRecord),
            baseAssetCode = source.baseAsset.id,
            quoteAssetCode = source.quoteAsset.id
    )
}