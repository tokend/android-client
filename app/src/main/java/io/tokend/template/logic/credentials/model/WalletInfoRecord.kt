package io.tokend.template.logic.credentials.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.tokend.crypto.ecdsa.erase
import org.tokend.sdk.keyserver.models.KdfAttributes
import org.tokend.sdk.keyserver.models.LoginParams
import org.tokend.sdk.keyserver.models.WalletCreationResult
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.wallet.Account

data class WalletInfoRecord(
    @JsonProperty("wallet_id")
    val walletId: String,
    @JsonProperty("account_id")
    val accountId: String,
    @JsonProperty("login")
    val login: String,
    @JsonProperty("login_params")
    val loginParams: LoginParams,
    @JsonProperty("seeds")
    var seeds: List<CharArray>
) {
    constructor(walletInfo: WalletInfo) : this(
        walletId = walletInfo.walletIdHex,
        accountId = walletInfo.accountId,
        login = walletInfo.email,
        loginParams = walletInfo.loginParams,
        seeds = walletInfo.secretSeeds
    )

    constructor(walletCreationResult: WalletCreationResult) : this(
        accountId = walletCreationResult.creationData.attributes.accountId,
        seeds = walletCreationResult.accounts.map { it.secretSeed!! },
        walletId = walletCreationResult.walletId,
        login = walletCreationResult.creationData.attributes.email,
        loginParams = walletCreationResult.loginParams
    )

    constructor(
        accountId: String,
        login: String
    ) : this(
        accountId = accountId,
        login = login,
        walletId = "",
        loginParams = LoginParams(
            "", 0,
            KdfAttributes("", 0, 0, 0, 0, byteArrayOf())
        ),
        seeds = emptyList()
    )

    @JsonIgnore
    var seedsAreErased = false
        private set

    fun withoutSeeds(): WalletInfoRecord =
        copy(seeds = emptyList())

    fun toSdkWalletInfo() = WalletInfo(
        accountId = accountId,
        email = login,
        loginParams = loginParams,
        walletIdHex = walletId,
        secretSeeds = seeds
    )

    /**
     * @see eraseSeeds
     */
    @JsonIgnore
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