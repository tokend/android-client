package org.tokend.template.features.kyc.model

import org.tokend.template.features.kyc.model.form.KycForm

sealed class KycState {

    sealed class Submitted<FormType : KycForm>(val formData: FormType,
                                               val requestId: Long) : KycState() {

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

    object Empty : KycState()
}