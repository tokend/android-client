package io.tokend.template.features.assets.buy.view.adapter

import io.tokend.template.data.model.AtomicSwapAskRecord
import io.tokend.template.features.assets.model.Asset
import java.math.BigDecimal

class AtomicSwapAskListItem(
    val available: BigDecimal,
    val asset: Asset,
    val quoteAssets: List<AtomicSwapAskRecord.QuoteAsset>,
    val source: AtomicSwapAskRecord?
) {
    constructor(source: AtomicSwapAskRecord) : this(
        available = source.amount,
        asset = source.asset,
        quoteAssets = source.quoteAssets.toList(),
        source = source
    )
}