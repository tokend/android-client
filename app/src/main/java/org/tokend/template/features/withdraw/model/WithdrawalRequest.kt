package org.tokend.template.features.withdraw.model

import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.history.SimpleFeeRecord
import java.io.Serializable
import java.math.BigDecimal

data class WithdrawalRequest(
        val accountId: String,
        val amount: BigDecimal,
        val asset: Asset,
        val balanceId: String,
        val destinationAddress: String,
        val fee: SimpleFeeRecord
) : Serializable