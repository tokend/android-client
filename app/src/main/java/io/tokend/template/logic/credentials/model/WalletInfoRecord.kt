package io.tokend.template.logic.credentials.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.tokend.sdk.keyserver.models.DecryptedWallet
import org.tokend.sdk.keyserver.models.KdfAttributes
import org.tokend.sdk.keyserver.models.LoginParams
import org.tokend.sdk.keyserver.models.WalletCreationResult
import org.tokend.wallet.Account

data class WalletInfoRecord
@JsonCreator
constructor(
    @JsonProperty("wallet_id")
    val walletId: String,
    @JsonProperty("account_id")
    val accountId: String,
    @JsonProperty("login")
    val login: String,
    @JsonProperty("login_params")
    val loginParams: LoginParams,
) {
    @get:JsonIgnore
    var accounts: List<Account> = emptyList()

    constructor(
        walletId: String,
        accountId: String,
        login: String,
        loginParams: LoginParams,
        accounts: List<Account>
    ): this(walletId, accountId, login, loginParams) {
        this.accounts = accounts
    }

    constructor(decryptedWallet: DecryptedWallet) : this(
        walletId = decryptedWallet.walletId,
        accountId = decryptedWallet.accountId,
        login = decryptedWallet.login,
        loginParams = decryptedWallet.loginParams,
        accounts = decryptedWallet.accounts
    )

    constructor(walletCreationResult: WalletCreationResult) : this(
        accountId = walletCreationResult.dataToSave.accountId,
        accounts = walletCreationResult.accounts,
        walletId = walletCreationResult.walletId,
        login = walletCreationResult.dataToSave.email,
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
        accounts = emptyList()
    )

    fun toSdkWallet() = DecryptedWallet(
        accountId = accountId,
        login = login,
        loginParams = loginParams,
        walletId = walletId,
        accounts = accounts
    )
}