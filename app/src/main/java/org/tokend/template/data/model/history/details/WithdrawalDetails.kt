package org.tokend.template.data.model.history.details

import org.tokend.sdk.api.generated.resources.OpCreateWithdrawRequestDetailsResource

class WithdrawalDetails(
        val destinationAddress: String
): BalanceChangeDetails() {
    constructor(op: OpCreateWithdrawRequestDetailsResource): this(
            destinationAddress = op.externalDetails.get("address").asText()
    )
}