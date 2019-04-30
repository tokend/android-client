package org.tokend.template.features.kyc.model.form

import com.google.gson.annotations.SerializedName
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.documents.model.DocumentType

/**
 * KYC form data with documents
 */
abstract class KycForm(
        @SerializedName("documents")
        val documents: MutableMap<String, RemoteFile> = mutableMapOf()
) {
    open fun getDocument(type: DocumentType): RemoteFile? {
        return documents[type.name.toLowerCase()]
    }

    open fun setDocument(type: DocumentType, file: RemoteFile?) {
        if (file == null) {
            documents.remove(type.name.toLowerCase())
            return
        }

        documents[type.name.toLowerCase()] = file
    }
}