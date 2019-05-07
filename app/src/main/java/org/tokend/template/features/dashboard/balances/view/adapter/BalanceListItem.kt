package org.tokend.template.features.dashboard.balances.view.adapter

import org.tokend.template.data.model.BalanceRecord
import java.math.BigDecimal

class BalanceListItem(
        val assetCode: String,
        val available: BigDecimal,
        val converted: BigDecimal?,
        val conversionAssetCode: String?,
        val assetName: String?,
        val logoUrl: String?,
        val source: BalanceRecord?
) {
    val displayedName: String = assetName ?: assetCode

    constructor(source: BalanceRecord) : this(
            assetCode = source.assetCode,
            available = source.available,
            logoUrl = source.asset.logoUrl,
            assetName = source.asset.name,
            converted = source.convertedAmount,
            conversionAssetCode = source.conversionAssetCode,
            source = source
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BalanceListItem) return false

        if (assetCode != other.assetCode) return false
        if (available != other.available) return false
        if (converted != other.converted) return false
        if (conversionAssetCode != other.conversionAssetCode) return false
        if (assetName != other.assetName) return false
        if (logoUrl != other.logoUrl) return false
        if (displayedName != other.displayedName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = assetCode.hashCode()
        result = 31 * result + available.hashCode()
        result = 31 * result + (converted?.hashCode() ?: 0)
        result = 31 * result + (conversionAssetCode?.hashCode() ?: 0)
        result = 31 * result + (assetName?.hashCode() ?: 0)
        result = 31 * result + (logoUrl?.hashCode() ?: 0)
        result = 31 * result + displayedName.hashCode()
        return result
    }

}