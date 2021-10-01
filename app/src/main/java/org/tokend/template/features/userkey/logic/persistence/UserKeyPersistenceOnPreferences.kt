package org.tokend.template.features.userkey.logic.persistence

import android.content.SharedPreferences
import android.os.Build
import org.tokend.crypto.ecdsa.erase
import org.tokend.template.logic.persistence.SecureStorage
import org.tokend.wallet.utils.toByteArray
import org.tokend.wallet.utils.toCharArray

/**
 * Saves user key in [SecureStorage].
 * Only operational on Android versions > [Build.VERSION_CODES.M]
 */
class UserKeyPersistenceOnPreferences(
        preferences: SharedPreferences
): UserKeyPersistence {
    private val secureStorage = SecureStorage(preferences)

    override fun save(key: CharArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val keyBytes = key.toByteArray()
            secureStorage.save(keyBytes, STORAGE_KEY)
            keyBytes.erase()
        }
    }

    override fun load(): CharArray? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val keyBytes = secureStorage
                    .load(STORAGE_KEY)

            if (keyBytes == null) {
                null
            } else {
                val key = keyBytes.toCharArray()
                keyBytes.erase()
                key
            }
        } else {
            null
        }
    }

    private companion object {
        const val STORAGE_KEY = "userkey"
    }
}