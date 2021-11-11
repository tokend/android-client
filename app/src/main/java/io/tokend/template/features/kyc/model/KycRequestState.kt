package io.tokend.template.features.kyc.model

import io.tokend.template.features.account.data.model.ResolvedAccountRole

sealed class KycRequestState {

    sealed class Submitted<FormType : KycForm>(
        val formData: FormType,
        val requestId: Long,
        val roleToSet: ResolvedAccountRole
    ) : KycRequestState() {

        class Pending<FormType : KycForm>(
            formData: FormType,
            requestId: Long,
            roleToSet: ResolvedAccountRole
        ) : Submitted<FormType>(formData, requestId, roleToSet)

        class Rejected<FormType : KycForm>(
            formData: FormType,
            requestId: Long,
            roleToSet: ResolvedAccountRole,
            val rejectReason: String?
        ) : Submitted<FormType>(formData, requestId, roleToSet)

        class PermanentlyRejected<FormType : KycForm>(
            formData: FormType,
            requestId: Long,
            roleToSet: ResolvedAccountRole,
            val rejectReason: String?
        ) : Submitted<FormType>(formData, requestId, roleToSet)

        class Approved<FormType : KycForm>(
            formData: FormType,
            requestId: Long,
            roleToSet: ResolvedAccountRole
        ) : Submitted<FormType>(formData, requestId, roleToSet)

        // Block is a change role request initiated by the master
        // setting the role to 'blocked'.
        class ApprovedToBlock(
            requestId: Long,
            roleToSet: ResolvedAccountRole,
            val blockReason: String?
        ) : Submitted<KycForm.Empty>(KycForm.Empty, requestId, roleToSet)
    }

    object Empty : KycRequestState()
}