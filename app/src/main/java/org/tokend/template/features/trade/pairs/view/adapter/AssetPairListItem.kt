package org.tokend.template.features.trade.pairs.view.adapter

import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.AssetPairRecord
import java.math.BigDecimal

data class AssetPairListItem(
        val baseAsset: Asset,
        val quoteAsset: Asset,
        val price: BigDecimal,
        val baseAssetLogoUrl: String?,
        val id: String?,
        val source: AssetPairRecord?
) {
    constructor(source: AssetPairRecord) : this(
            baseAsset = source.base,
            quoteAsset = source.quote,
            price = source.price,
            baseAssetLogoUrl = source.baseAssetLogoUrl,
            id = source.id,
            source = source
    )
}