package org.tokend.template.features.kyc.model

sealed class KycRequestState {

    sealed class Submitted<FormType : KycForm>(
        val formData: FormType,
        val requestId: Long,
        val roleToSet: Long
    ) : KycRequestState() {

        class Pending<FormType : KycForm>(
            formData: FormType,
            requestId: Long,
            roleToSet: Long
        ) : Submitted<FormType>(formData, requestId, roleToSet)

        class Rejected<FormType : KycForm>(
            formData: FormType,
            requestId: Long,
            roleToSet: Long,
            val rejectReason: String
        ) : Submitted<FormType>(formData, requestId, roleToSet)

        class PermanentlyRejected<FormType : KycForm>(
            formData: FormType,
            requestId: Long,
            roleToSet: Long,
            val rejectReason: String
        ) : Submitted<FormType>(formData, requestId, roleToSet)

        class Approved<FormType : KycForm>(
            formData: FormType,
            requestId: Long,
            roleToSet: Long
        ) : Submitted<FormType>(formData, requestId, roleToSet)
    }

    object Empty : KycRequestState()
}