package org.tokend.template.features.send.amount.model

import org.tokend.template.features.amountscreen.model.AmountInputResult
import org.tokend.template.features.balances.model.BalanceRecord
import org.tokend.template.features.send.model.PaymentFee
import java.math.BigDecimal

class PaymentAmountData(
        amount: BigDecimal,
        balance: BalanceRecord,
        val description: String?,
        val fee: PaymentFee
) : AmountInputResult(amount, balance)