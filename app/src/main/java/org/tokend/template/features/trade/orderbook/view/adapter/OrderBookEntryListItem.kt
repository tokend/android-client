package org.tokend.template.features.trade.orderbook.view.adapter

import org.tokend.template.features.trade.orderbook.model.OrderBookEntryRecord
import java.math.BigDecimal
import java.math.MathContext

class OrderBookEntryListItem(
        val isBuy: Boolean,
        val price: BigDecimal,
        val volume: BigDecimal,
        val baseAssetCode: String,
        val quoteAssetCode: String,
        val maxVolume: BigDecimal
) {
    val volumePercentsOfMax =
            (volume * BigDecimal(100)).divide(maxVolume, MathContext.DECIMAL128).toInt()

    constructor(record: OrderBookEntryRecord,
                maxVolume: BigDecimal): this(
            isBuy = record.isBuy,
            price = record.price,
            volume = record.volume,
            maxVolume = maxVolume,
            baseAssetCode = record.baseAssetCode,
            quoteAssetCode = record.quoteAssetCode
    )
}