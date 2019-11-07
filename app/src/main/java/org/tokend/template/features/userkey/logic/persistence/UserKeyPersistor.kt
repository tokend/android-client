package org.tokend.template.features.userkey.logic.persistence

import org.tokend.crypto.ecdsa.erase
import org.tokend.template.logic.credentials.SimpleCredentialsProvider

interface UserKeyPersistor : SimpleCredentialsProvider {
    fun save(key: CharArray)
    fun load(): CharArray?

    override fun hasSimpleCredentials(): Boolean {
        val key = load()
        return if (key != null) {
            key.erase()
            true
        } else {
            false
        }
    }

    override fun getSimpleCredentials(): Pair<String, CharArray> {
        return "account@local.device" to load()!!
    }
}