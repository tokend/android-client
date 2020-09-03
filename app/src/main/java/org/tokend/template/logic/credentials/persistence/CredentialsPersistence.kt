package org.tokend.template.logic.credentials.persistence

import org.tokend.template.logic.credentials.CredentialsProvider

interface CredentialsPersistence : CredentialsProvider {
    /**
     * @param email that is retrieved from corresponding WalletInfo
     * @param password password for encryption
     */
    fun saveCredentials(email:String, password: CharArray)

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

    override fun hasCredentials(): Boolean {
        return getSavedEmail() != null && hasSavedPassword()
    }

    override fun getCredentials(): Pair<String, CharArray> {
        return getSavedEmail()!! to getSavedPassword()!!
    }
}