package io.tokend.template.features.history.model

import org.tokend.sdk.api.generated.inner.Fee
import org.tokend.sdk.api.generated.resources.CalculatedFeeResource
import org.tokend.wallet.NetworkParams
import java.io.Serializable
import java.math.BigDecimal

class SimpleFeeRecord(
    val fixed: BigDecimal,
    val percent: BigDecimal
) : Serializable {
    constructor(feeResponse: Fee) : this(feeResponse.fixed, feeResponse.calculatedPercent)

    constructor(feeResponse: CalculatedFeeResource) : this(
        feeResponse.fixed,
        feeResponse.calculatedPercent
    )

    val total = fixed + percent

    fun toXdrFee(networkParams: NetworkParams): org.tokend.wallet.xdr.Fee {
        return org.tokend.wallet.xdr.Fee(
            networkParams.amountToPrecised(fixed),
            networkParams.amountToPrecised(percent),
            org.tokend.wallet.xdr.Fee.FeeExt.EmptyVersion()
        )
    }

    companion object {
        val ZERO = SimpleFeeRecord(BigDecimal.ZERO, BigDecimal.ZERO)
    }
}