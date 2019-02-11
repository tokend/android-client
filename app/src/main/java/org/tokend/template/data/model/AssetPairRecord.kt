package org.tokend.template.data.model

import org.tokend.sdk.api.generated.resources.AssetPairResource
import org.tokend.sdk.utils.HashCodes
import org.tokend.template.util.PolicyChecker
import org.tokend.wallet.xdr.AssetPairPolicy
import java.math.BigDecimal

class AssetPairRecord(
        val base: String,
        val quote: String,
        val price: BigDecimal,
        val policy: Int = 0
) : PolicyChecker {

    val code = "$base:$quote"

    fun isTradeable(): Boolean {
        return checkPolicy(policy, AssetPairPolicy.TRADEABLE_SECONDARY_MARKET.value)
    }

    override fun equals(other: Any?): Boolean {
        return other is AssetPairRecord
                && other.code == this.code
                && other.base == this.base
                && other.quote == this.quote
                && other.price == this.price
                && other.policy == this.policy
    }

    override fun hashCode(): Int {
        return HashCodes.ofMany(code, base, quote, price, policy)
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