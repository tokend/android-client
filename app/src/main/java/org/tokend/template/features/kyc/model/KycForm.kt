package org.tokend.template.features.kyc.model

import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.blobs.model.Blob
import org.tokend.sdk.api.documents.model.DocumentType

/**
 * KYC form data with documents
 */
sealed class KycForm(
        @SerializedName("documents")
        val documents: MutableMap<String, RemoteFile>? = mutableMapOf()
) {
    open fun getDocument(type: DocumentType): RemoteFile? {
        return documents?.get(type.name.toLowerCase())
    }

    open fun setDocument(type: DocumentType, file: RemoteFile?) {
        if (file == null) {
            documents?.remove(type.name.toLowerCase())
            return
        }

        documents?.put(type.name.toLowerCase(), file)
    }

    class Corporate(documents: MutableMap<String, RemoteFile>,
                    @SerializedName(COMPANY_KEY)
                    val company: String
    ) : KycForm(documents) {
        val avatar: RemoteFile?
            get() = documents?.get("kyc_avatar")

        companion object {
            const val COMPANY_KEY = "company"
        }
    }

    class General(@SerializedName(FIRST_NAME_KEY)
                  val firstName: String,
                  @SerializedName("last_name")
                  val lastName: String
    ) : KycForm(null) {
        val avatar: RemoteFile?
            get() = documents?.get("kyc_avatar")

        companion object {
            const val FIRST_NAME_KEY = "first_name"
        }
    }

    /**
     * Empty form to use in case when the original form
     * can't be processed
     */
    object Empty : KycForm()

    companion object {
        fun fromBlob(blob: Blob): KycForm {
            val valueJson = JSONObject(blob.valueString)

            val isGeneral = valueJson.has(General.FIRST_NAME_KEY)
            val isCorporate = valueJson.has(Corporate.COMPANY_KEY)

            return when {
                isCorporate ->
                    blob.getValue(Corporate::class.java)
                isGeneral ->
                    blob.getValue(General::class.java)
                else ->
                    throw IllegalArgumentException("Unknown KYC form type")
            }
        }
    }
}