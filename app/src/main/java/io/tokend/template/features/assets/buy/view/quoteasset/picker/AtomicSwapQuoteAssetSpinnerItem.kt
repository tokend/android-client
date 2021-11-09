package io.tokend.template.features.assets.buy.view.quoteasset.picker

import io.tokend.template.data.model.AtomicSwapAskRecord
import io.tokend.template.features.assets.model.Asset
import org.tokend.sdk.utils.BigDecimalUtil
import java.math.BigDecimal

class AtomicSwapQuoteAssetSpinnerItem(
    val asset: Asset,
    val total: BigDecimal
) {
    constructor(
        source: AtomicSwapAskRecord.QuoteAsset,
        amount: BigDecimal
    ) : this(
        asset = source,
        total = BigDecimalUtil.scaleAmount(source.price * amount, source.trailingDigits)
    )
}