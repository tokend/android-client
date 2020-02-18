package org.tokend.template.features.dashboard.balances.view.adapter

import org.tokend.template.features.assets.model.Asset
import org.tokend.template.features.balances.model.BalanceRecord
import java.math.BigDecimal

class BalanceListItem(
        val asset: Asset,
        val available: BigDecimal,
        val converted: BigDecimal?,
        val conversionAsset: Asset?,
        val assetName: String?,
        val logoUrl: String?,
        val source: BalanceRecord?
) {
    val displayedName: String = assetName ?: asset.code

    constructor(source: BalanceRecord) : this(
            asset = source.asset,
            available = source.available,
            logoUrl = source.asset.logoUrl,
            assetName = source.asset.name,
            converted = source.convertedAmount,
            conversionAsset = source.conversionAsset,
            source = source
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BalanceListItem) return false

        if (asset != other.asset) return false
        if (available != other.available) return false
        if (converted != other.converted) return false
        if (conversionAsset != other.conversionAsset) return false
        if (assetName != other.assetName) return false
        if (logoUrl != other.logoUrl) return false
        if (displayedName != other.displayedName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = asset.hashCode()
        result = 31 * result + available.hashCode()
        result = 31 * result + (converted?.hashCode() ?: 0)
        result = 31 * result + (conversionAsset?.hashCode() ?: 0)
        result = 31 * result + (assetName?.hashCode() ?: 0)
        result = 31 * result + (logoUrl?.hashCode() ?: 0)
        result = 31 * result + displayedName.hashCode()
        return result
    }

}