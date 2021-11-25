package io.tokend.template.features.trade.pairs.model

import com.fasterxml.jackson.databind.ObjectMapper
import io.tokend.template.data.model.RecordWithLogo
import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.assets.model.AssetRecord
import io.tokend.template.features.assets.model.SimpleAsset
import io.tokend.template.features.urlconfig.model.UrlConfig
import io.tokend.template.util.RecordWithPolicy
import org.tokend.sdk.api.v3.model.generated.resources.AssetPairResource
import org.tokend.wallet.xdr.AssetPairPolicy
import java.io.Serializable
import java.math.BigDecimal

class AssetPairRecord(
    val base: Asset,
    val quote: Asset,
    val price: BigDecimal,
    override val policy: Int,
    override val logoUrl: String?
) : Serializable, RecordWithPolicy, RecordWithLogo {
    val id = "$base:$quote"

    fun isTradeable(): Boolean {
        return hasPolicy(AssetPairPolicy.TRADEABLE_SECONDARY_MARKET.value)
    }

    override fun equals(other: Any?): Boolean {
        return other is AssetPairRecord && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        @JvmStatic
        fun fromResource(
            resource: AssetPairResource,
            urlConfig: UrlConfig?,
            objectMapper: ObjectMapper
        ): AssetPairRecord {
            return AssetPairRecord(
                base = SimpleAsset(resource.baseAsset),
                quote = SimpleAsset(resource.quoteAsset),
                price = resource.price,
                policy = resource.policies.value
                    ?: throw IllegalStateException("Asset pair must have a policy"),
                logoUrl =
                if (resource.baseAsset.isFilled)
                    AssetRecord.fromResource(resource.baseAsset, urlConfig, objectMapper)
                        .logoUrl
                else
                    null
            )
        }
    }
}