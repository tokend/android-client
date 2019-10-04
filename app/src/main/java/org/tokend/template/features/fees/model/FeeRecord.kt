package org.tokend.template.features.fees.model

import org.tokend.sdk.api.generated.resources.FeeRecordResource
import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.SimpleAsset
import java.io.Serializable
import java.math.BigDecimal

class FeeRecord(
        val feeType: Int,
        val subtype: Int,
        val asset: Asset,
        val fixed: BigDecimal,
        val percent: BigDecimal,
        val lowerBound: BigDecimal = BigDecimal.ZERO,
        val upperBound: BigDecimal = BigDecimal.ZERO
) : Serializable {
    constructor(source: FeeRecordResource) : this(
            feeType = source.appliedTo.feeType,
            subtype = source.appliedTo.subtype.toInt(),
            asset = SimpleAsset(source.appliedTo.asset),
            fixed = source.fixed,
            percent = source.percent,
            lowerBound = source.appliedTo.lowerBound,
            upperBound = source.appliedTo.upperBound
    )

    val total: BigDecimal
        get() = fixed + percent

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FeeRecord) return false

        if (feeType != other.feeType) return false
        if (asset != other.asset) return false
        if (fixed != other.fixed) return false
        if (percent != other.percent) return false
        if (lowerBound != other.lowerBound) return false

        return true
    }

    override fun hashCode(): Int {
        var result = feeType
        result = 31 * result + asset.hashCode()
        result = 31 * result + fixed.hashCode()
        result = 31 * result + percent.hashCode()
        result = 31 * result + lowerBound.hashCode()
        return result
    }
}