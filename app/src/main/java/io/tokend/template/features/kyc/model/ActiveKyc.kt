package io.tokend.template.features.kyc.model

/**
 * Represents active (approved) account KYC.
 */
sealed class ActiveKyc {
    object Missing : ActiveKyc()
    class Form(val formData: KycForm) : ActiveKyc()
}