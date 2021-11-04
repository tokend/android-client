package org.tokend.template.logic.credentials.persistence

import io.reactivex.Maybe
import io.reactivex.rxkotlin.toMaybe
import org.tokend.template.logic.credentials.model.WalletInfoRecord

interface WalletInfoPersistence {
    /**
     * Saves given [walletInfo] by [login] and encrypts it with [password]
     *
     * @param walletInfo [WalletInfoRecord] with filled [WalletInfoRecord.seeds] field.
     * @param password password for encryption
     */
    fun saveWalletInfo(
        walletInfo: WalletInfoRecord,
        password: CharArray
    )

    /**
     * @return saved data, null if there is no saved data for [login]
     * or [password] is incorrect
     */
    fun loadWalletInfo(login: String, password: CharArray): WalletInfoRecord?

    /**
     * @see loadWalletInfo
     */
    fun loadWalletInfoMaybe(login: String, password: CharArray): Maybe<WalletInfoRecord> =
        Maybe.defer {
            loadWalletInfo(login, password).toMaybe()
        }

    /**
     * Clears stored data
     */
    fun clearWalletInfo(login: String)
}