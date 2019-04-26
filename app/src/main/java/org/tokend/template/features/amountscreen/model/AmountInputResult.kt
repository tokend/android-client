package org.tokend.template.features.amountscreen.model

import java.math.BigDecimal

open class AmountInputResult(
        val amount: BigDecimal,
        val assetCode: String
)