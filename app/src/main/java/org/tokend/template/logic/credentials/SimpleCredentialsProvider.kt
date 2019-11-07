package org.tokend.template.logic.credentials

interface SimpleCredentialsProvider {
    fun hasSimpleCredentials(): Boolean
    fun getSimpleCredentials(): Pair<String, CharArray>
}