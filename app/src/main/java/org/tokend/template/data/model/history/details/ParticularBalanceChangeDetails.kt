package org.tokend.template.data.model.history.details

import org.tokend.sdk.api.generated.inner.ParticularBalanceChange
import org.tokend.template.data.model.history.SimpleFeeRecord
import java.math.BigDecimal

class ParticularBalanceChangeDetails(
        val amount: BigDecimal,
        val fee: SimpleFeeRecord,
        val balanceId: String,
        val assetCode: String
) {
    constructor(particularBalanceChangeResponse: ParticularBalanceChange) :
            this(
                    amount = particularBalanceChangeResponse.amount,
                    fee = SimpleFeeRecord(
                            particularBalanceChangeResponse.fee
                    ),
                    balanceId = particularBalanceChangeResponse.balanceAddress,
                    assetCode = particularBalanceChangeResponse.assetCode
            )
}