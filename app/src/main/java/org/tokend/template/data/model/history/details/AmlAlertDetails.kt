package org.tokend.template.data.model.history.details

import org.tokend.sdk.api.generated.resources.OpCreateAMLAlertRequestDetailsResource

class AmlAlertDetails(
        val reason: String?
): BalanceChangeDetails() {
    constructor(op: OpCreateAMLAlertRequestDetailsResource): this(
            reason = op.reason.takeIf { it.isNotEmpty() }
    )
}