package io.tokend.template.logic.credentials.providers

interface CredentialsProvider {
    fun hasCredentials(): Boolean
    fun getCredentials(): Pair<String, CharArray>
}