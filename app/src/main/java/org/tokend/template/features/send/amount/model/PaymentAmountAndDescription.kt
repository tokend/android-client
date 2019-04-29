package org.tokend.template.features.send.amount.model

import org.tokend.template.features.amountscreen.model.AmountInputResult
import java.math.BigDecimal

class PaymentAmountAndDescription(
        amount: BigDecimal,
        assetCode: String,
        val description: String?) : AmountInputResult(amount, assetCode)