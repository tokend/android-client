package io.tokend.template.features.userkey.logic.persistence

import io.tokend.template.logic.credentials.providers.CredentialsProvider
import org.tokend.crypto.ecdsa.erase

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