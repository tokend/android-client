package org.tokend.template.data.model

import org.tokend.sdk.api.accounts.model.SimpleBalanceDetails
import org.tokend.template.extensions.Asset
import java.math.BigDecimal

// TODO: Asset model
class BalanceRecord(
        val id: String,
        val asset: Asset,
        val available: BigDecimal,
        val availableConverted: BigDecimal,
        val conversionAssetCode: String,
        val locked: BigDecimal
) {
    constructor(source: SimpleBalanceDetails) : this(
            id = source.balanceId,
            available = source.balance,
            locked = source.lockedBalance,
            availableConverted = source.convertedBalance,
            asset = source.assetDetails!!,
            conversionAssetCode = source.conversionAsset
    )

    val assetCode: String
        get() = asset.code
}