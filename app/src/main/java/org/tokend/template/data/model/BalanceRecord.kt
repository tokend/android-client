package org.tokend.template.data.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.tokend.sdk.api.generated.resources.BalanceResource
import org.tokend.sdk.api.generated.resources.ConvertedBalanceStateResource
import java.io.Serializable
import java.math.BigDecimal
import java.math.MathContext

class BalanceRecord(
        val id: String,
        val asset: AssetRecord,
        val available: BigDecimal,
        val conversionAssetCode: String?,
        val convertedAmount: BigDecimal?
) : Serializable {
    constructor(source: BalanceResource, urlConfig: UrlConfig?, mapper: ObjectMapper) : this(
            id = source.id,
            available = source.state.available,
            asset = AssetRecord.fromResource(source.asset, urlConfig, mapper),
            // TODO: Change to real values
            conversionAssetCode = "USD",
            convertedAmount = source.state.available.multiply(BigDecimal("13.4"), MathContext.DECIMAL128)
    )

    constructor(source: ConvertedBalanceStateResource,
                urlConfig: UrlConfig?,
                mapper: ObjectMapper) : this(
            id = source.balance.id,
            available = source.initialAmounts.available,
            asset = AssetRecord.fromResource(source.asset, urlConfig, mapper),
            conversionAssetCode = "USD",
            convertedAmount =
            if (source.isConverted)
                source.convertedAmounts.available
            else
                null
    )

    val assetCode: String
        get() = asset.code
}