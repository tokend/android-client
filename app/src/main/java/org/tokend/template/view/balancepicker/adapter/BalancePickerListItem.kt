package org.tokend.template.view.balancepicker.adapter

import org.tokend.template.data.model.BalanceRecord
import java.math.BigDecimal

class BalancePickerListItem(
        val assetCode: String,
        val available: BigDecimal,
        val isEnough: Boolean,
        val assetName: String?,
        val logoUrl: String?,
        val source: BalanceRecord?
) {
    constructor(source: BalanceRecord,
                available: BigDecimal = source.available,
                required: BigDecimal = BigDecimal.ZERO) : this(
            assetCode = source.assetCode,
            available = available,
            isEnough = available >= required,
            logoUrl = source.asset.logoUrl,
            assetName = source.asset.name,
            source = source
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BalancePickerListItem) return false

        if (assetCode != other.assetCode) return false
        if (available != other.available) return false
        if (isEnough != other.isEnough) return false
        if (assetName != other.assetName) return false
        if (logoUrl != other.logoUrl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = assetCode.hashCode()
        result = 31 * result + available.hashCode()
        result = 31 * result + isEnough.hashCode()
        result = 31 * result + (assetName?.hashCode() ?: 0)
        result = 31 * result + (logoUrl?.hashCode() ?: 0)
        return result
    }

}
