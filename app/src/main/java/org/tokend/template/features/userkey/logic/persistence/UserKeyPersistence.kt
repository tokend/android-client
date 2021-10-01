package org.tokend.template.features.userkey.logic.persistence

import org.tokend.crypto.ecdsa.erase
import org.tokend.template.logic.credentials.CredentialsProvider

interface UserKeyPersistence : CredentialsProvider {
    fun save(key: CharArray)
    fun load(): CharArray?

    override fun hasCredentials(): Boolean {
        val key = load()
        return if (key != null) {
            key.erase()
            true
        } else {
            false
        }
    }

    override fun getCredentials(): Pair<String, CharArray> {
        return "account@local.device" to load()!!
    }
}