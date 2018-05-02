package org.tokend.template.base.view.adapter.history

import org.tokend.sdk.api.models.transactions.Transaction
import java.math.BigDecimal

class TxHistoryItem(
        val amount: BigDecimal,
        val asset: String,
        val action: String, // TODO: Use proper type
        val counterparty: String?,
        val state: Transaction.PaymentState,
        val isReceived: Boolean
)