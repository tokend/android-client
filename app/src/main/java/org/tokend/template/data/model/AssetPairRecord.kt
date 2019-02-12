package org.tokend.template.data.model

import org.tokend.sdk.api.assets.model.AssetPair
import org.tokend.sdk.api.generated.resources.AssetPairResource
import org.tokend.template.util.PolicyChecker
import org.tokend.wallet.xdr.AssetPairPolicy
import java.io.Serializable
import java.math.BigDecimal

class AssetPairRecord(
        val base: String,
        val quote: String,
        val price: BigDecimal,
        val policy: Int = 0
) : Serializable, PolicyChecker {
    val id = "$base:$quote"

    constructor(source: AssetPair) : this(
            base = source.base,
            quote = source.quote,
            price = source.price,
            policy = source.policy
    )

    fun isTradeable(): Boolean {
        return checkPolicy(policy, AssetPairPolicy.TRADEABLE_SECONDARY_MARKET.value)
    }

    override fun equals(other: Any?): Boolean {
        return other is AssetPairRecord && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        @JvmStatic
        fun fromResource(resource: AssetPairResource): AssetPairRecord {
            return AssetPairRecord(
                    base = resource.baseAsset.id,
                    quote = resource.quoteAsset.id,
                    price = resource.price,
                    policy = resource.policies.value
            )
        }
    }
}