package io.tokend.template.logic.credentials.persistence

import android.content.SharedPreferences
import android.os.Build
import io.tokend.template.data.storage.persistence.SecureStorage
import org.tokend.wallet.utils.toByteArray
import org.tokend.wallet.utils.toCharArray

/**
 * Represents secure credentials storage based on SharedPreferences.
 */
class CredentialsPersistenceImpl(
    private val preferences: SharedPreferences
) : CredentialsPersistence {
    private val secureStorage = SecureStorage(preferences)

    override fun saveCredentials(login: String, password: CharArray) {

        tryToSavePassword(password)

        preferences.edit().putString(LOGIN_KEY, login).apply()
    }

    override fun getSavedLogin(): String? {
        return preferences.getString(LOGIN_KEY, "")
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

    override fun clear(keepLogin: Boolean) {
        secureStorage.clear(PASSWORD_KEY)

        if (!keepLogin) {
            preferences.edit().remove(LOGIN_KEY).apply()
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
        private const val LOGIN_KEY = "email"
    }
}