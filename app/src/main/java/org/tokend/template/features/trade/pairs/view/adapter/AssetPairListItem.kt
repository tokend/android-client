package org.tokend.template.features.trade.pairs.view.adapter

import android.content.Context
import org.tokend.template.R
import org.tokend.template.data.model.AssetPairRecord
import java.math.BigDecimal

data class AssetPairListItem(
        val code: String,
        val baseAssetCode: String,
        val quoteAssetCode: String,
        val price: BigDecimal,
        val source: AssetPairRecord?
) {
    constructor(source: AssetPairRecord,
                context: Context) : this(
            code = context.getString(R.string.template_asset_pair),
            baseAssetCode = source.base,
            quoteAssetCode = source.quote,
            price = source.price,
            source = source
    )
}