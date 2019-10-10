package org.tokend.template.features.userkey.logic

import android.content.SharedPreferences
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.logic.persistance.CredentialsPersistor

class UserKeyPersistor(
        preferences: SharedPreferences
) : CredentialsPersistor(preferences) {
    override fun saveCredentials(credentials: WalletInfo, password: CharArray) {
        throw NotImplementedError("Persistor for user key can't hold credentials")
    }

    override fun getSavedEmail(): String? = "userkey@local.device"

    fun saveUserKeyAsPassword(userKey: CharArray) {
        tryToSavePassword(userKey)
    }
}