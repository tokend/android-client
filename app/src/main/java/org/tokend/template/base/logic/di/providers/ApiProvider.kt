package org.tokend.template.base.logic.di.providers

import org.tokend.sdk.api.ApiService
import org.tokend.sdk.keyserver.KeyStorage

interface ApiProvider {
    fun getApi(): ApiService
    fun getSignedApi(): ApiService?
    fun getKeyStorage(): KeyStorage
    fun getSignedKeyStorage(): KeyStorage?
}