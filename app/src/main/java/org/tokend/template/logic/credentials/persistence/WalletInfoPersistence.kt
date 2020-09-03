package org.tokend.template.logic.credentials.persistence

import io.reactivex.Maybe
import io.reactivex.rxkotlin.toMaybe
import org.tokend.sdk.keyserver.models.WalletInfo

interface WalletInfoPersistence {
    /**
     * @param data [WalletInfo] with filled [WalletInfo.secretSeed] field.
     * @param password password for encryption
     */
    fun saveWalletInfo(data: WalletInfo, password: CharArray)

    /**
     * @return saved data, null if there is no saved data or password is incorrect
     */
    fun loadWalletInfo(email: String, password: CharArray): WalletInfo?

    /**
     * @see loadWalletInfo
     */
    fun loadWalletInfoMaybe(email: String, password: CharArray): Maybe<WalletInfo> = Maybe.defer {
        loadWalletInfo(email, password).toMaybe()
    }

    /**
     * Clears stored data
     */
    fun clear()

}