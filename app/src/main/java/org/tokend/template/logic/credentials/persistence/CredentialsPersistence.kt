package org.tokend.template.logic.credentials.persistence

import org.tokend.template.logic.credentials.providers.CredentialsProvider

interface CredentialsPersistence : CredentialsProvider {
    /**
     * @param login that is retrieved from corresponding WalletInfo
     * @param password password for encryption
     */
    fun saveCredentials(login: String, password: CharArray)

    /**
     * @return saved email or null if it's missing
     */
    fun getSavedLogin(): String?

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
     * @param keepLogin if set then login will not be cleared
     *
     * @see getSavedLogin
     */
    fun clear(keepLogin: Boolean)

    override fun hasCredentials(): Boolean {
        return getSavedLogin() != null && hasSavedPassword()
    }

    override fun getCredentials(): Pair<String, CharArray> {
        return getSavedLogin()!! to getSavedPassword()!!
    }
}