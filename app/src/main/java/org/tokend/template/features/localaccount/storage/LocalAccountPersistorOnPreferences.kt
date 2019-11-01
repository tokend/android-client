package org.tokend.template.features.localaccount.storage

import android.content.SharedPreferences
import com.google.gson.annotations.SerializedName
import org.tokend.sdk.factory.GsonFactory
import org.tokend.sdk.utils.extentions.decodeHex
import org.tokend.sdk.utils.extentions.encodeHexString
import org.tokend.template.features.localaccount.model.LocalAccount

class LocalAccountPersistorOnPreferences(
        private val preferences: SharedPreferences
) : LocalAccountPersistor {
    private class LocalAccountData(
            @SerializedName("account_id")
            val accountId: String,
            @SerializedName("serialized_encrypted_source_hex")
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

    override fun load(): LocalAccount? {
        return preferences
                .getString(KEY, "")
                .takeIf(String::isNotEmpty)
                ?.let { GsonFactory().getBaseGson().fromJson(it, LocalAccountData::class.java) }
                ?.let(LocalAccountData::toLocalAccount)
    }

    override fun save(localAccount: LocalAccount) {
        preferences
                .edit()
                .putString(
                        KEY,
                        GsonFactory().getBaseGson().toJson(
                                LocalAccountData.fromLocalAccount(localAccount)
                        )
                )
                .apply()
    }

    override fun clear() {
        preferences.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "local_account"
    }
}