package org.tokend.template.base.logic.persistance

import android.content.SharedPreferences
import android.os.Build
import org.tokend.sdk.api.ApiFactory
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.extensions.toByteArray
import org.tokend.template.extensions.toCharArray

class CredentialsPersistor(
        private val preferences: SharedPreferences
) {
    val secureStorage = SecureStorage(preferences)

    /**
     * @param credentials [WalletInfo] with filled [WalletInfo.secretSeed] field.
     * @param password password for encryption
     */
    fun saveCredentials(credentials: WalletInfo, password: CharArray) {
        val email = credentials.email
        val nonSensitiveData =
                ApiFactory.getBaseGson().toJson(
                        credentials.copy(secretSeed = CharArray(0))
                ).toByteArray()
        val sensitiveData = credentials.secretSeed.toByteArray()

        secureStorage.saveWithPassword(sensitiveData, SEED_KEY, password)
        secureStorage.saveWithPassword(nonSensitiveData, WALLET_INFO_KEY, password)

        if (Build.VERSION.SDK_INT >= 23) {
            val passwordBytes = password.toByteArray()
            secureStorage.save(passwordBytes, PASSWORD_KEY)
            passwordBytes.fill(0)
        }

        preferences.edit().putString(EMAIL_KEY, email).apply()
    }

    fun hasCredentials(email: String): Boolean {
        return getSavedEmail() == email
    }

    fun getSavedEmail(): String? {
        return preferences.getString(EMAIL_KEY, "")
                .takeIf { it.isNotEmpty() }
    }

    fun loadCredentials(password: CharArray): WalletInfo? {
        try {
            val walletInfoBytes = secureStorage.loadWithPassword(WALLET_INFO_KEY, password)
                    ?: return null
            val walletInfo = ApiFactory.getBaseGson()
                    .fromJson(String(walletInfoBytes), WalletInfo::class.java)

            val seedBytes = secureStorage.loadWithPassword(SEED_KEY, password)
                    ?: return null
            walletInfo.secretSeed = seedBytes.toCharArray()
            seedBytes.fill(0)

            return walletInfo
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun clear(keepEmail: Boolean) {
        secureStorage.clear(SEED_KEY)
        secureStorage.clear(WALLET_INFO_KEY)
        secureStorage.clear(PASSWORD_KEY)

        if (!keepEmail) {
            preferences.edit().remove(EMAIL_KEY).apply()
        }
    }

    companion object {
        private const val SEED_KEY = "(◕‿◕✿)"
        private const val WALLET_INFO_KEY = "ಠ_ಠ"
        private const val PASSWORD_KEY = "(¬_¬)"
        private val EMAIL_KEY = "email"
    }
}