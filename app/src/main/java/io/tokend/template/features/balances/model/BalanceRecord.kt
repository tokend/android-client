package io.tokend.template.features.balances.model

import com.fasterxml.jackson.databind.ObjectMapper
import io.tokend.template.extensions.equalsArithmetically
import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.assets.model.AssetRecord
import io.tokend.template.features.urlconfig.model.UrlConfig
import org.tokend.sdk.api.generated.resources.BalanceResource
import org.tokend.sdk.api.generated.resources.ConvertedBalanceStateResource
import org.tokend.sdk.utils.BigDecimalUtil
import java.io.Serializable
import java.math.BigDecimal
import java.math.MathContext

class BalanceRecord(
    val id: String,
    val asset: AssetRecord,
    var available: BigDecimal,
    val conversionAsset: Asset?,
    var convertedAmount: BigDecimal?,
    val conversionPrice: BigDecimal?
    /* Do not forget about contentEquals */
) : Serializable {
    constructor(source: BalanceResource, urlConfig: UrlConfig?, mapper: ObjectMapper) : this(
        id = source.id,
        available = source.state.available,
        asset = AssetRecord.fromResource(source.asset, urlConfig, mapper),
        conversionAsset = null,
        convertedAmount = null,
        conversionPrice = null
    )

    constructor(
        source: ConvertedBalanceStateResource,
        urlConfig: UrlConfig?,
        mapper: ObjectMapper,
        conversionAsset: Asset?
    ) : this(
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
            if (source.price == null || source.price.signum() == 0)
                if (source.initialAmounts.available.signum() > 0)
                    source.convertedAmounts.available
                        .divide(
                            source.initialAmounts.available,
                            MathContext.DECIMAL64
                        )
                        .let {
                            BigDecimalUtil.scaleAmount(
                                it, conversionAsset?.trailingDigits
                                    ?: 6
                            )
                        }
                else
                    BigDecimal.ONE
            else
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

    fun contentEquals(other: BalanceRecord): Boolean {
        return available.equalsArithmetically(other.available)
                && asset.contentEquals(other.asset)
                &&
                (conversionAsset == other.conversionAsset
                        || conversionAsset != null && other.conversionAsset != null
                        && conversionAsset.contentEquals(other.conversionAsset))
                && convertedAmount.equalsArithmetically(other.convertedAmount)
                && conversionPrice.equalsArithmetically(other.conversionPrice)
    }
}