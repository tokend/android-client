package org.tokend.template.data.model

import org.tokend.sdk.api.assets.model.AssetPair
import org.tokend.sdk.utils.HashCodes
import org.tokend.template.util.PolicyChecker
import org.tokend.wallet.xdr.AssetPairPolicy
import java.io.Serializable
import java.math.BigDecimal

class AssetPairRecord(
        val base: String,
        val quote: String,
        val price: BigDecimal,
        val physicalPrice: BigDecimal,
        val policy: Int = 0
) : Serializable, PolicyChecker {
    constructor(source: AssetPair) : this(
            base = source.base,
            quote = source.quote,
            price = source.price,
            physicalPrice = source.physicalPrice,
            policy = source.policy
    )

    fun isTradeable(): Boolean {
        return checkPolicy(policy, AssetPairPolicy.TRADEABLE_SECONDARY_MARKET.value)
    }

    override fun equals(other: Any?): Boolean {
        return other is AssetPairRecord
                && other.base == this.base
                && other.quote == this.quote
                && other.price == this.price
                && other.physicalPrice == physicalPrice
                && other.policy == this.policy
    }

    override fun hashCode(): Int {
        return HashCodes.ofMany(base, quote, price, physicalPrice, policy)
    }
}