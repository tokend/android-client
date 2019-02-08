package org.tokend.template.data.model.history

import org.tokend.template.data.model.history.details.BalanceChangeDetails
import java.math.BigDecimal
import java.util.*

open class BalanceChange(
        val id: String,
        val action: BalanceChangeAction,
        val amount: BigDecimal,
        val assetCode: String,
        val fee: SimpleFeeRecord,
        val date: Date,
        val details: BalanceChangeDetails
) {
    override fun equals(other: Any?): Boolean {
        return other is BalanceChange && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}