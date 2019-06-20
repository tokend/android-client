package org.tokend.template.features.assets.buy.view.adapter

import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.AtomicSwapAskRecord
import java.math.BigDecimal

class AtomicSwapAskListItem(
        val available: BigDecimal,
        val asset: Asset,
        val quoteAssets: List<AtomicSwapAskRecord.QuoteAsset>,
        val source: AtomicSwapAskRecord?
) {
    constructor(source: AtomicSwapAskRecord): this(
            available = source.amount,
            asset = source.asset,
            quoteAssets = source.quoteAssets.toList(),
            source = source
    )
}