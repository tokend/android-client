package org.tokend.template.features.amountscreen.model

import org.tokend.template.features.assets.model.Asset
import org.tokend.template.features.balances.model.BalanceRecord
import java.math.BigDecimal

open class AmountInputResult(
        val amount: BigDecimal,
        val asset: Asset,
        val balance: BalanceRecord? = null
) {
    constructor(amount: BigDecimal, balance: BalanceRecord):
            this(amount, balance.asset, balance)
}