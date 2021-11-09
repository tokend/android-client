package io.tokend.template.features.send.model

import io.tokend.template.features.assets.model.Asset
import org.tokend.sdk.utils.extentions.encodeBase64String
import java.io.Serializable
import java.math.BigDecimal
import java.security.SecureRandom

data class PaymentRequest(
    val amount: BigDecimal,
    val asset: Asset,
    val senderAccountId: String,
    val senderBalanceId: String,
    val recipient: PaymentRecipient,
    val fee: PaymentFee,
    val paymentSubject: String?,
    val reference: String = SecureRandom.getSeed(16).encodeBase64String()
) : Serializable