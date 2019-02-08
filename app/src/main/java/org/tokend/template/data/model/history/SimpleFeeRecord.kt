package org.tokend.template.data.model.history

import org.tokend.sdk.api.generated.inner.Fee
import java.math.BigDecimal

class SimpleFeeRecord(
        val fixed: BigDecimal,
        val percent: BigDecimal
) {
    constructor(feeResponse: Fee): this(feeResponse.fixed, feeResponse.calculatedPercent)

    val total = fixed + percent
}