package org.tokend.template.logic.credentials.model

import com.google.gson.annotations.SerializedName
import org.tokend.crypto.ecdsa.erase
import org.tokend.sdk.keyserver.models.LoginParams
import org.tokend.sdk.keyserver.models.WalletCreateResult
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.wallet.Account

data class WalletInfoRecord(
    @SerializedName("wallet_id")
    var walletId: String,
    @SerializedName("account_id")
    val accountId: String,
    @SerializedName("login_params")
    var loginParams: LoginParams,
    @SerializedName("seeds")
    var seeds: List<CharArray>
) {
    constructor(walletInfo: WalletInfo) : this(
        walletId = walletInfo.walletIdHex,
        accountId = walletInfo.accountId,
        loginParams = walletInfo.loginParams,
        seeds = walletInfo.secretSeeds
    )

    constructor(walletCreateResult: WalletCreateResult) : this(
        accountId = walletCreateResult.walletData.attributes.accountId,
        seeds = walletCreateResult.accounts.map { it.secretSeed!! },
        walletId = walletCreateResult.walletId,
        loginParams = walletCreateResult.loginParams
    )

    var seedsAreErased = false
        private set

    fun withoutSeeds(): WalletInfoRecord =
        copy(seeds = emptyList())

    fun toSdkWalletInfo(login: String) = WalletInfo(
        accountId = accountId,
        email = login,
        loginParams = loginParams,
        walletIdHex = walletId,
        secretSeeds = seeds
    )

    /**
     * @see eraseSeeds
     */
    fun getAccounts(): List<Account> =
        if (!seedsAreErased)
            seeds.map(Account.Companion::fromSecretSeed)
        else
            throw IllegalStateException("Seeds are erased")

    fun eraseSeeds() {
        seeds.forEach(CharArray::erase)
        seedsAreErased = true
    }
}