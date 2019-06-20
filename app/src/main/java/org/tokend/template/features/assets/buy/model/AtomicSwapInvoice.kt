package org.tokend.template.features.assets.buy.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)
class AtomicSwapInvoice
@JsonCreator
constructor(
        @JsonProperty("address")
        val address: String,
        @JsonProperty("amount")
        val amount: BigDecimal,
        @JsonProperty("asset")
        val assetCode: String
)