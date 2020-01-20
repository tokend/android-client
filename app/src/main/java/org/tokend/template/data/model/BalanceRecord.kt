package org.tokend.template.data.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.tokend.sdk.api.generated.resources.BalanceResource
import org.tokend.sdk.api.generated.resources.ConvertedBalanceStateResource
import java.io.Serializable
import java.math.BigDecimal

class BalanceRecord(
        val id: String,
        val asset: AssetRecord,
        var available: BigDecimal,
        val conversionAsset: Asset?,
        var convertedAmount: BigDecimal?,
        val conversionPrice: BigDecimal?
) : Serializable {
    constructor(source: BalanceResource, urlConfig: UrlConfig?, mapper: ObjectMapper) : this(
            id = source.id,
            available = source.state.available,
            asset = AssetRecord.fromResource(source.asset, urlConfig, mapper),
            conversionAsset = null,
            convertedAmount = null,
            conversionPrice = null
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
                null,
            conversionPrice =
            if (source.isConverted)
                source.price
            else
                null
    )

    val assetCode: String
        get() = asset.code

    override fun equals(other: Any?): Boolean {
        return other is BalanceRecord && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}