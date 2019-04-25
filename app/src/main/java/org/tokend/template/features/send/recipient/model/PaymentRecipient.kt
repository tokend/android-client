package org.tokend.template.features.send.recipient.model

import org.tokend.template.features.send.model.PaymentRequest

/**
 * Recipient for [PaymentRequest]
 */
data class PaymentRecipient(
        val accountId: String,
        val nickname: String? = null
)