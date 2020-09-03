package org.tokend.template.logic.credentials

interface CredentialsProvider {
    fun hasCredentials(): Boolean
    fun getCredentials(): Pair<String, CharArray>
}