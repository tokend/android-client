package org.tokend.template.logic.credentials.persistence

import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.logic.credentials.SimpleCredentialsProvider

interface CredentialsPersistence : SimpleCredentialsProvider {
    /**
     * @param credentials [WalletInfo] with filled [WalletInfo.secretSeed] field.
     * @param password password for encryption
     */
    fun saveCredentials(credentials: WalletInfo, password: CharArray)

    /**
     * @return saved email or null if it's missing
     */
    fun getSavedEmail(): String?

    /**
     * @return true if there is a securely saved password
     */
    fun hasSavedPassword(): Boolean

    /**
     * @return saved password or null if it's missing
     */
    fun getSavedPassword(): CharArray?

    /**
     * Clears stored credentials
     *
     * @param keepEmail if set then email will not be cleared
     *
     * @see getSavedEmail
     */
    fun clear(keepEmail: Boolean)

    override fun hasSimpleCredentials(): Boolean {
        return getSavedEmail() != null && hasSavedPassword()
    }

    override fun getSimpleCredentials(): Pair<String, CharArray> {
        return getSavedEmail()!! to getSavedPassword()!!
    }
}