package io.tokend.template.features.history.model.details

import io.tokend.template.features.history.model.SimpleFeeRecord
import org.tokend.sdk.api.generated.inner.ParticularBalanceChangeEffect
import java.io.Serializable
import java.math.BigDecimal

class ParticularBalanceChangeDetails(
    val amount: BigDecimal,
    val fee: SimpleFeeRecord,
    val balanceId: String,
    val assetCode: String
) : Serializable {
    constructor(particularBalanceChangeResponse: ParticularBalanceChangeEffect) :
            this(
                amount = particularBalanceChangeResponse.amount,
                fee = SimpleFeeRecord(
                    particularBalanceChangeResponse.fee
                ),
                balanceId = particularBalanceChangeResponse.balanceAddress,
                assetCode = particularBalanceChangeResponse.assetCode
            )
}