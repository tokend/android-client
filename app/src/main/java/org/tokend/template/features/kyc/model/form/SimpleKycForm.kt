package org.tokend.template.features.kyc.model.form

import com.google.gson.annotations.SerializedName
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.documents.model.DocumentType

/**
 * KYC form with avatar only and some data required to obtain its type
 */
class SimpleKycForm(documents: MutableMap<String, RemoteFile>,
                    @SerializedName("company")
                    val company: String?
) : KycForm(documents) {
    val avatar: RemoteFile?
        get() = documents?.get("kyc_avatar")

    val formType: KycFormType
        get() = when {
            getDocument(DocumentType.KYC_SELFIE) != null -> KycFormType.GENERAL
            company != null -> KycFormType.CORPORATE
            else -> KycFormType.UNKNOWN
        }
}