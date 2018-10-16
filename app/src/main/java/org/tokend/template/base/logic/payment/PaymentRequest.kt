package org.tokend.template.base.logic.payment

import org.tokend.sdk.api.fees.model.Fee
import java.io.Serializable
import java.math.BigDecimal

data class PaymentRequest(
        val amount: BigDecimal,
        val asset: String,
        val senderBalanceId: String,
        val recipientAccountId: String,
        val recipientNickname: String,
        val senderFee: Fee,
        val recipientFee: Fee,
        val paymentSubject: String?
) : Serializable {
    var senderPaysRecipientFee = false
}