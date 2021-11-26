package io.tokend.template.features.kyc.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.tokend.template.features.account.data.model.AccountRole
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.blobs.model.Blob
import org.tokend.sdk.factory.JsonApiTools

/**
 * KYC form data with documents
 */
sealed class KycForm(
    @param:JsonProperty("documents")
    @get:JsonProperty("documents")
    var documents: MutableMap<String, RemoteFile>? = mutableMapOf()
) {
    @get:JsonIgnore
    abstract val role: AccountRole

    class Corporate(
        documents: MutableMap<String, RemoteFile>,
        @JsonProperty("company")
        val company: String
    ) : KycForm(documents), KycFormWithAvatar {
        override val avatar: RemoteFile?
            get() = documents?.get("kyc_avatar")

        override val role: AccountRole = ROLE

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Corporate

            if (company != other.company) return false

            return true
        }

        override fun hashCode(): Int {
            return company.hashCode()
        }

        companion object {
            val ROLE = AccountRole.CORPORATE
        }
    }

    class General(
        @JsonProperty("first_name")
        override val firstName: String,
        @JsonProperty("last_name")
        override val lastName: String
    ) : KycForm(null), KycFormWithName, KycFormWithAvatar {
        @get:JsonIgnore
        override val avatar: RemoteFile?
            get() = documents?.get(AVATAR_DOCUMENT_KEY)

        @get:JsonIgnore
        override val fullName: String
            get() = "$firstName $lastName"

        override val role: AccountRole = ROLE

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as General

            if (firstName != other.firstName) return false
            if (lastName != other.lastName) return false

            return true
        }

        override fun hashCode(): Int {
            var result = firstName.hashCode()
            result = 31 * result + lastName.hashCode()
            return result
        }

        companion object {
            const val AVATAR_DOCUMENT_KEY = "kyc_avatar"
            val ROLE = AccountRole.GENERAL
        }
    }

    object Empty : KycForm() {
        override val role: AccountRole
            get() {
                throw IllegalArgumentException("You can't use empty form to change role")
            }
    }

    companion object {
        /**
         * @return KYC form parsed from [blob] corresponding to the [accountRole]
         *
         * @param blob KYC form blob
         * @param accountRole role of the account to find out proper form type
         */
        fun fromBlob(
            blob: Blob,
            accountRole: AccountRole,
        ): KycForm {
            return fromJson(blob.valueString, accountRole)
        }

        /**
         * @return KYC form parsed from [json] corresponding to the [accountRole]
         *
         * @param json KYC form JSON
         * @param accountRole role of the account to find out proper form type
         */
        fun fromJson(
            json: String,
            accountRole: AccountRole
        ): KycForm {
            return fromJson(JsonApiTools.objectMapper.readTree(json), accountRole)
        }

        /**
         * @return KYC form parsed from [json] corresponding to the [accountRole]
         *
         * @param json KYC form JSON
         * @param accountRole role of the account to find out proper form type
         */
        fun fromJson(
            json: JsonNode,
            accountRole: AccountRole
        ): KycForm {
            val mapper = JsonApiTools.objectMapper

            return when (accountRole) {
                General.ROLE ->
                    mapper.treeToValue(json, General::class.java)
                Corporate.ROLE ->
                    mapper.treeToValue(json, Corporate::class.java)
                else ->
                    throw IllegalArgumentException("Don't know which KYC form to use for role $accountRole")
            }
        }
    }
}