package org.tokend.template.features.trade.orderbook.model

import org.tokend.sdk.api.generated.resources.OrderBookEntryResource
import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.SimpleAsset
import java.math.BigDecimal

class OrderBookEntryRecord(
        val price: BigDecimal,
        val volume: BigDecimal,
        val baseAsset: Asset,
        val quoteAsset: Asset,
        val isBuy: Boolean
) {
    constructor(source: OrderBookEntryResource): this(
            price = source.price,
            volume = source.cumulativeBaseAmount,
            isBuy = source.isBuy,
            baseAsset = SimpleAsset(source.baseAsset),
            quoteAsset = SimpleAsset(source.quoteAsset)
    )
}