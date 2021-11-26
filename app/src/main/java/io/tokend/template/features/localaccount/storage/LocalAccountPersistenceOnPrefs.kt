package io.tokend.template.features.localaccount.storage

import android.content.SharedPreferences
import com.fasterxml.jackson.annotation.JsonProperty
import io.tokend.template.data.storage.persistence.ObjectPersistenceOnPrefs
import io.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.sdk.utils.extentions.decodeHex
import org.tokend.sdk.utils.extentions.encodeHexString

class LocalAccountPersistenceOnPrefs(
    preferences: SharedPreferences
) : ObjectPersistenceOnPrefs<LocalAccount>(LocalAccount::class.java, preferences, KEY) {
    private class LocalAccountData(
        @JsonProperty("account_id")
        val accountId: String,
        @JsonProperty("serialized_encrypted_source_hex")
        val serializedEncryptedSourceHex: String
    ) {
        fun toLocalAccount(): LocalAccount {
            return LocalAccount.fromSerializedEncryptedSource(
                accountId = accountId,
                serializedEncryptedSource = serializedEncryptedSourceHex.decodeHex()
            )
        }

        companion object {
            fun fromLocalAccount(localAccount: LocalAccount): LocalAccountData {
                return LocalAccountData(
                    accountId = localAccount.accountId,
                    serializedEncryptedSourceHex = localAccount
                        .serializeEncryptedSource()
                        ?.encodeHexString()
                        ?: throw IllegalArgumentException("Cannot save unencrypted local account")
                )
            }
        }
    }

    override fun serializeItem(item: LocalAccount): String {
        return mapper.writeValueAsString(LocalAccountData.fromLocalAccount(item))
    }

    override fun deserializeItem(serialized: String): LocalAccount? {
        return try {
            mapper.readValue(serialized, LocalAccountData::class.java).toLocalAccount()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private companion object {
        const val KEY = "local_account"
    }
}