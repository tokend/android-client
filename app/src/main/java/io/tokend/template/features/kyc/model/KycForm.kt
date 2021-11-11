package io.tokend.template.features.kyc.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import io.tokend.template.features.account.data.model.AccountRole
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.blobs.model.Blob
import org.tokend.sdk.factory.GsonFactory

/**
 * KYC form data with documents
 */
sealed class KycForm(
    @SerializedName("documents")
    var documents: MutableMap<String, RemoteFile>? = mutableMapOf()
) {
    // In order to avoid serialization it is declared as a method.
    abstract fun getRole(): AccountRole

    class Corporate(
        documents: MutableMap<String, RemoteFile>,
        @SerializedName("company")
        val company: String
    ) : KycForm(documents), KycFormWithAvatar {
        override val avatar: RemoteFile?
            get() = documents?.get("kyc_avatar")

        override fun getRole() = ROLE

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
        @SerializedName("first_name")
        override val firstName: String,
        @SerializedName("last_name")
        override val lastName: String
    ) : KycForm(null), KycFormWithName, KycFormWithAvatar {
        override val avatar: RemoteFile?
            get() = documents?.get(AVATAR_DOCUMENT_KEY)

        override val fullName: String
            get() = "$firstName $lastName"

        override fun getRole() = ROLE

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
        override fun getRole(): AccountRole {
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
         * Finds out KYC form type by the name of corresponding [roleId]
         *
         * @param json KYC form JSON
         */
        fun fromJson(
            json: String,
            accountRole: AccountRole
        ): KycForm {
            return fromJson(
                GsonFactory().getBaseGson()
                    .fromJson(json, JsonElement::class.java), accountRole
            )
        }

        /**
         * Finds out KYC form type by the name of corresponding [roleId]
         *
         * @param json KYC form JSON
         */
        fun fromJson(
            json: JsonElement,
            accountRole: AccountRole
        ): KycForm {
            val gson = GsonFactory().getBaseGson()

            return when (accountRole) {
                General.ROLE ->
                    gson.fromJson(json, General::class.java)
                Corporate.ROLE ->
                    gson.fromJson(json, Corporate::class.java)
                else ->
                    throw IllegalArgumentException("Don't know which KYC form to use for role $accountRole")
            }
        }
    }
}