package org.tokend.template.features.fees.model

import org.tokend.sdk.api.fees.model.Fee
import java.io.Serializable
import java.math.BigDecimal

class FeeRecord(
        val feeType: Int,
        val subtype: Int,
        val requestAsset: String,
        val asset: String,
        val fixed: BigDecimal,
        val percent: BigDecimal,
        val lowerBound: BigDecimal,
        val upperBound: BigDecimal
): Serializable {
    constructor(source: Fee) : this(
            feeType = source.feeType,
            subtype = source.subtype,
            requestAsset = source.requestAsset,
            asset = source.asset,
            fixed = source.fixed,
            percent = source.percent,
            lowerBound = source.lowerBound,
            upperBound = source.upperBound
    )

    val total: BigDecimal
        get() = fixed + percent

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FeeRecord) return false

        if (feeType != other.feeType) return false
        if (requestAsset != other.requestAsset) return false
        if (asset != other.asset) return false
        if (fixed != other.fixed) return false
        if (percent != other.percent) return false
        if (lowerBound != other.lowerBound) return false

        return true
    }

    override fun hashCode(): Int {
        var result = feeType
        result = 31 * result + requestAsset.hashCode()
        result = 31 * result + asset.hashCode()
        result = 31 * result + fixed.hashCode()
        result = 31 * result + percent.hashCode()
        result = 31 * result + lowerBound.hashCode()
        return result
    }
}