package io.tokend.template.features.kyc.model

import com.google.gson.annotations.SerializedName
import io.tokend.template.features.keyvalue.model.KeyValueEntryRecord
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
    abstract fun getRoleKey(): String

    class Corporate(
        documents: MutableMap<String, RemoteFile>,
        @SerializedName(COMPANY_KEY)
        val company: String
    ) : KycForm(documents), KycFormWithAvatar {
        override val avatar: RemoteFile?
            get() = documents?.get("kyc_avatar")

        override fun getRoleKey() = General.ROLE_KEY

        override fun equals(other: Any?): Boolean {
            return other is Corporate && this.company == other.company && this.documents == other.documents
        }

        override fun hashCode(): Int {
            return company.hashCode() + documents.hashCode()
        }

        companion object {
            const val ROLE_KEY = "$ROLE_KEY_PREFIX:corporate"
            const val COMPANY_KEY = "company"
        }
    }

    class General(
        @SerializedName(FIRST_NAME_KEY)
        override val firstName: String,
        @SerializedName("last_name")
        override val lastName: String
    ) : KycForm(null), KycFormWithName, KycFormWithAvatar {
        override val avatar: RemoteFile?
            get() = documents?.get(AVATAR_DOCUMENT_KEY)

        override val fullName: String
            get() = "$firstName $lastName"

        override fun getRoleKey() = ROLE_KEY

        override fun equals(other: Any?): Boolean {
            return other is General &&
                    this.firstName == other.firstName &&
                    this.lastName == other.lastName &&
                    this.documents == other.documents
        }

        override fun hashCode(): Int {
            return firstName.hashCode() + lastName.hashCode() + documents.hashCode()
        }

        companion object {
            const val ROLE_KEY = "$ROLE_KEY_PREFIX:general"
            const val FIRST_NAME_KEY = "first_name"
            const val AVATAR_DOCUMENT_KEY = "kyc_avatar"
        }
    }

    /**
     * Empty form to use in case when the original form
     * can't be processed
     */
    object Empty : KycForm() {
        override fun getRoleKey(): String {
            throw IllegalArgumentException("You can't use empty form to change role")
        }
    }

    companion object {
        private const val ROLE_KEY_PREFIX = "account_role"

        /**
         * Finds out KYC form type by the name of corresponding [roleId]
         *
         * @param blob KYC form blob
         */
        fun fromBlob(
            blob: Blob,
            roleId: Long,
            keyValueEntries: Collection<KeyValueEntryRecord>
        ): KycForm {
            return fromJson(blob.valueString, roleId, keyValueEntries)
        }

        /**
         * Finds out KYC form type by the name of corresponding [roleId]
         *
         * @param json KYC form JSON
         */
        fun fromJson(
            json: String,
            roleId: Long,
            keyValueEntries: Collection<KeyValueEntryRecord>
        ): KycForm {
            val gson = GsonFactory().getBaseGson()
            val roleKey = keyValueEntries
                .find {
                    it.key.startsWith(ROLE_KEY_PREFIX)
                            && it is KeyValueEntryRecord.Number
                            && it.value == roleId
                }
                ?.key
                ?: throw IllegalArgumentException("Role $roleId has no corresponding key-value entry")

            return when (roleKey) {
                General.ROLE_KEY ->
                    gson.fromJson(json, General::class.java)
                Corporate.ROLE_KEY ->
                    gson.fromJson(json, Corporate::class.java)
                else ->
                    // Replace with your custom "wrong role" exception
                    throw IllegalArgumentException("Unknown KYC form type")
            }
        }
    }
}