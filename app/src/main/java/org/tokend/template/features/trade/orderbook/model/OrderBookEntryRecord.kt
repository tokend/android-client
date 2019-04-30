package org.tokend.template.features.trade.orderbook.model

import org.tokend.sdk.api.generated.resources.OrderBookEntryResource
import java.math.BigDecimal

class OrderBookEntryRecord(
        val price: BigDecimal,
        val volume: BigDecimal,
        val baseAssetCode: String,
        val quoteAssetCode: String,
        val isBuy: Boolean
) {
    constructor(source: OrderBookEntryResource): this(
            price = source.price,
            volume = source.cumulativeBaseAmount,
            isBuy = source.isBuy,
            baseAssetCode = source.baseAsset.id,
            quoteAssetCode = source.quoteAsset.id
    )
}