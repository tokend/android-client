package org.tokend.template.data.model.history.details

import org.tokend.sdk.api.generated.resources.OpCreateIssuanceRequestDetailsResource

class IssuanceDetails(
        val cause: String?,
        val reference: String?
): BalanceChangeDetails() {
    constructor(op: OpCreateIssuanceRequestDetailsResource): this(
            cause = op.externalDetails?.get("cause")?.asText(),
            reference = op.reference
    )
}