package org.tokend.template.di.providers

import org.tokend.sdk.api.TokenDApi
import org.tokend.sdk.keyserver.KeyStorage

interface ApiProvider {
    fun getApi(): TokenDApi
    fun getSignedApi(): TokenDApi?
    fun getKeyStorage(): KeyStorage
    fun getSignedKeyStorage(): KeyStorage?
}