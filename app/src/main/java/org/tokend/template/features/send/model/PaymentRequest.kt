package org.tokend.template.features.send.model

import org.tokend.sdk.utils.extentions.encodeBase64String
import org.tokend.template.features.fees.model.FeeRecord
import java.io.Serializable
import java.math.BigDecimal
import java.security.SecureRandom

data class PaymentRequest(
        val amount: BigDecimal,
        val asset: String,
        val senderAccountId: String,
        val senderBalanceId: String,
        val recipientAccountId: String,
        val recipientNickname: String,
        val senderFee: FeeRecord,
        val recipientFee: FeeRecord,
        val paymentSubject: String?,
        val reference: String = SecureRandom.getSeed(16).encodeBase64String()
) : Serializable {
    var senderPaysRecipientFee = false
}