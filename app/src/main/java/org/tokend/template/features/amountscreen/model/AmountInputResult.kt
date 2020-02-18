package org.tokend.template.features.amountscreen.model

import org.tokend.template.features.assets.model.Asset
import java.math.BigDecimal

open class AmountInputResult(
        val amount: BigDecimal,
        val asset: Asset
)