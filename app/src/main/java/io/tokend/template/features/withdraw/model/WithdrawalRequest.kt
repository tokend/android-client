package io.tokend.template.features.withdraw.model

import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.history.model.SimpleFeeRecord
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