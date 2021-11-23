package io.tokend.template.features.history.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.tokend.sdk.api.generated.inner.Fee
import org.tokend.sdk.api.generated.resources.CalculatedFeeResource
import org.tokend.wallet.NetworkParams
import java.io.Serializable
import java.math.BigDecimal

class SimpleFeeRecord(
    @JsonProperty("fixed")
    val fixed: BigDecimal,
    @JsonProperty("percent")
    val percent: BigDecimal
) : Serializable {
    constructor(feeResponse: Fee) : this(feeResponse.fixed, feeResponse.calculatedPercent)

    constructor(feeResponse: CalculatedFeeResource) : this(
        feeResponse.fixed,
        feeResponse.calculatedPercent
    )

    @JsonIgnore
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