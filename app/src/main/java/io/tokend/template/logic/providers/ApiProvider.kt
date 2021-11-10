package io.tokend.template.logic.providers

import org.tokend.sdk.api.TokenDApi
import org.tokend.sdk.keyserver.KeyServer

interface ApiProvider {
    fun getApi(): TokenDApi
    fun getKeyServer(): KeyServer
    fun getSignedApi(): TokenDApi
    fun getSignedKeyServer(): KeyServer
}