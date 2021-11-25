package io.tokend.template.features.trade.orderbook.model

import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.assets.model.SimpleAsset
import org.tokend.sdk.api.v3.model.generated.resources.OrderBookEntryResource
import java.math.BigDecimal

class OrderBookEntryRecord(
    val price: BigDecimal,
    val volume: BigDecimal,
    val baseAsset: Asset,
    val quoteAsset: Asset,
    val isBuy: Boolean
) {
    constructor(source: OrderBookEntryResource) : this(
        price = source.price,
        volume = source.cumulativeBaseAmount,
        isBuy = source.isBuy,
        baseAsset = SimpleAsset(source.baseAsset),
        quoteAsset = SimpleAsset(source.quoteAsset)
    )
}