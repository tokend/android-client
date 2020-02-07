package org.tokend.template.features.kyc.model

sealed class KycRequestState {

    sealed class Submitted<FormType : KycForm>(val formData: FormType,
                                               val requestId: Long) : KycRequestState() {

        class Pending<FormType : KycForm>(formData: FormType,
                                          requestId: Long) : Submitted<FormType>(formData, requestId)

        class Rejected<FormType : KycForm>(
                formData: FormType,
                requestId: Long,
                val rejectReason: String
        ) : Submitted<FormType>(formData, requestId)

        class Approved<FormType : KycForm>(formData: FormType,
                                           requestId: Long) : Submitted<FormType>(formData, requestId)
    }

    object Empty : KycRequestState()
}