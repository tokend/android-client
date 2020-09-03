package org.tokend.template.logic.credentials.persistence

import android.content.SharedPreferences
import android.os.Build
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.logic.persistence.SecureStorage
import org.tokend.wallet.utils.toByteArray
import org.tokend.wallet.utils.toCharArray

/**
 * Represents secure credentials storage based on SharedPreferences.
 */
class CredentialsPersistenceImpl(
        private val preferences: SharedPreferences
): CredentialsPersistence {
    private val secureStorage = SecureStorage(preferences)

    override fun saveCredentials(credentials: WalletInfo, password: CharArray) {
        val email = credentials.email

        tryToSavePassword(password)

        preferences.edit().putString(EMAIL_KEY, email).apply()
    }

    override fun getSavedEmail(): String? {
        return preferences.getString(EMAIL_KEY, "")
                ?.takeIf { it.isNotEmpty() }
    }

    override fun hasSavedPassword(): Boolean {
        val password = getSavedPassword()
        val hasPassword = password != null
        password?.fill('0')
        return hasPassword
    }

    override fun getSavedPassword(): CharArray? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return null
        }

        val passwordBytes = secureStorage.load(PASSWORD_KEY)
                ?: return null
        val password = passwordBytes.toCharArray()
        passwordBytes.fill(0)
        return password
    }

    override fun clear(keepEmail: Boolean) {
        secureStorage.clear(PASSWORD_KEY)

        if (!keepEmail) {
            preferences.edit().remove(EMAIL_KEY).apply()
        }
    }

    private fun tryToSavePassword(password: CharArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val passwordBytes = password.toByteArray()
            secureStorage.save(passwordBytes, PASSWORD_KEY)
            passwordBytes.fill(0)
        }
    }

    companion object {
        const val PASSWORD_KEY = "(¬_¬)"
        private const val EMAIL_KEY = "email"
    }
}