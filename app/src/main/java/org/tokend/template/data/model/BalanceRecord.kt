package org.tokend.template.data.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.tokend.sdk.api.generated.resources.BalanceResource
import org.tokend.sdk.api.generated.resources.ConvertedBalanceStateResource
import java.io.Serializable
import java.math.BigDecimal

class BalanceRecord(
        val id: String,
        val asset: AssetRecord,
        val available: BigDecimal,
        val conversionAsset: Asset?,
        val convertedAmount: BigDecimal?
) : Serializable {
    constructor(source: BalanceResource, urlConfig: UrlConfig?, mapper: ObjectMapper) : this(
            id = source.id,
            available = source.state.available,
            asset = AssetRecord.fromResource(source.asset, urlConfig, mapper),
            conversionAsset = null,
            convertedAmount = null
    )

    constructor(source: ConvertedBalanceStateResource,
                urlConfig: UrlConfig?,
                mapper: ObjectMapper,
                conversionAsset: Asset?) : this(
            id = source.balance.id,
            available = source.initialAmounts.available,
            asset = AssetRecord.fromResource(source.balance.asset, urlConfig, mapper),
            conversionAsset = conversionAsset,
            convertedAmount =
            if (source.isConverted)
                source.convertedAmounts.available
            else
                null
    )

    val assetCode: String
        get() = asset.code
}