package io.tokend.template.features.send.model

import java.io.Serializable

/**
 * Recipient for [PaymentRequest]
 */
data class PaymentRecipient(
    val accountId: String,
    val nickname: String? = null
) : Serializable {
    /**
     * [nickname] if it's present, [accountId] otherwise
     */
    val displayedValue = nickname ?: accountId
}