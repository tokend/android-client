package org.tokend.template.features.withdraw.model

import org.tokend.template.features.fees.model.FeeRecord
import java.io.Serializable
import java.math.BigDecimal

data class WithdrawalRequest(
        val accountId: String,
        val amount: BigDecimal,
        val asset: String,
        val balanceId: String,
        val destinationAddress: String,
        val fee: FeeRecord
) : Serializable