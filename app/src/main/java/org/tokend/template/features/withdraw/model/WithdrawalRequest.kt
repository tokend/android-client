package org.tokend.template.features.withdraw.model

import org.tokend.sdk.api.fees.model.Fee
import java.io.Serializable
import java.math.BigDecimal

data class WithdrawalRequest(
        val accountId: String,
        val amount: BigDecimal,
        val asset: String,
        val destinationAddress: String,
        val fee: Fee
) : Serializable