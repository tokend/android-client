package org.tokend.template.features.trade.orderbook.view.adapter

import org.tokend.template.features.trade.orderbook.model.OrderBookEntryRecord
import java.math.BigDecimal

class OrderBookEntryListItem(
        val isBuy: Boolean,
        val price: BigDecimal,
        val volume: BigDecimal,
        val baseAssetCode: String,
        val quoteAssetCode: String
) {
    constructor(record: OrderBookEntryRecord): this(
            isBuy = record.isBuy,
            price = record.price,
            volume = record.volume,
            baseAssetCode = record.baseAssetCode,
            quoteAssetCode = record.quoteAssetCode
    )
}