package org.tokend.template.features.send.model

import org.tokend.template.data.model.history.SimpleFeeRecord

class PaymentFee(
        val senderFee: SimpleFeeRecord,
        val recipientFee: SimpleFeeRecord,
        var senderPaysForRecipient: Boolean = false
)