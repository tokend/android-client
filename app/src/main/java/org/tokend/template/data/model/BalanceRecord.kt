package org.tokend.template.data.model

import org.tokend.sdk.api.accounts.model.SimpleBalanceDetails
import org.tokend.template.features.assets.model.AssetRecord
import java.math.BigDecimal

class BalanceRecord(
        val id: String,
        val asset: AssetRecord,
        val available: BigDecimal,
        val availableConverted: BigDecimal,
        val conversionAssetCode: String,
        val locked: BigDecimal
) {
    constructor(source: SimpleBalanceDetails, urlConfig: UrlConfig?) : this(
            id = source.balanceId,
            available = source.balance,
            locked = source.lockedBalance,
            availableConverted = source.convertedBalance,
            asset = AssetRecord(source.assetDetails!!, urlConfig),
            conversionAssetCode = source.conversionAsset
    )

    val assetCode: String
        get() = asset.code
}