package org.tokend.template.features.assets.buy.view.quoteasset.picker

import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.features.assets.model.Asset
import org.tokend.template.data.model.AtomicSwapAskRecord
import java.math.BigDecimal

class AtomicSwapQuoteAssetSpinnerItem(
        val asset: Asset,
        val total: BigDecimal
) {
    constructor(source: AtomicSwapAskRecord.QuoteAsset,
                amount: BigDecimal): this(
            asset = source,
            total = BigDecimalUtil.scaleAmount(source.price * amount, source.trailingDigits)
    )
}