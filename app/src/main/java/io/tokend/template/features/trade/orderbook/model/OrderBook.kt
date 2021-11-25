package io.tokend.template.features.trade.orderbook.model

import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.assets.model.SimpleAsset
import org.tokend.sdk.api.v3.model.generated.resources.OrderBookResource
import java.math.BigDecimal

class OrderBook(
    val id: String,
    val buyEntries: List<OrderBookEntryRecord>,
    val sellEntries: List<OrderBookEntryRecord>,
    val baseAsset: Asset,
    val quoteAsset: Asset
) {
    val maxBuyVolume: BigDecimal = buyEntries.lastOrNull()?.volume ?: BigDecimal.ZERO

    val maxSellVolume: BigDecimal = sellEntries.lastOrNull()?.volume ?: BigDecimal.ZERO

    val maxVolume: BigDecimal = maxBuyVolume.max(maxSellVolume)

    constructor(source: OrderBookResource) : this(
        id = source.id,
        buyEntries = source.buyEntries.map(::OrderBookEntryRecord),
        sellEntries = source.sellEntries.map(::OrderBookEntryRecord),
        baseAsset = SimpleAsset(source.baseAsset),
        quoteAsset = SimpleAsset(source.quoteAsset)
    )
}