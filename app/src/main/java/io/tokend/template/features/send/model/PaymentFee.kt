package io.tokend.template.features.send.model

import io.tokend.template.features.history.model.SimpleFeeRecord
import java.io.Serializable
import java.math.BigDecimal

class PaymentFee(
    val senderFee: SimpleFeeRecord,
    val recipientFee: SimpleFeeRecord,
    var senderPaysForRecipient: Boolean = false
) : Serializable {
    val totalPercentSenderFee: BigDecimal
        get() = senderFee.percent +
                if (senderPaysForRecipient) recipientFee.percent else BigDecimal.ZERO

    val totalFixedSenderFee: BigDecimal
        get() = senderFee.fixed +
                if (senderPaysForRecipient) recipientFee.fixed else BigDecimal.ZERO

    val totalSenderFee: BigDecimal
        get() = totalFixedSenderFee + totalPercentSenderFee
}