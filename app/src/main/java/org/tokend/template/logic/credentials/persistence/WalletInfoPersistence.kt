package org.tokend.template.logic.credentials.persistence

import io.reactivex.Maybe
import io.reactivex.rxkotlin.toMaybe
import org.tokend.sdk.keyserver.models.WalletInfo

interface WalletInfoPersistence {
    fun saveWalletInfoData(credentials: WalletInfo, password: CharArray)

    fun loadWalletInfo(password: CharArray): WalletInfo?

    fun loadWalletInfoMaybe(password: CharArray): Maybe<WalletInfo> = Maybe.defer {
        loadWalletInfo(password).toMaybe()
    }

    fun clear(keepEmail: Boolean)
}