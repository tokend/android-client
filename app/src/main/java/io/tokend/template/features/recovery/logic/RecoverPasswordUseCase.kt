package io.tokend.template.features.recovery.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.tokend.template.logic.providers.ApiProvider
import org.tokend.rx.extensions.randomSingle
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.models.WalletCreateResult
import org.tokend.wallet.Account

/**
 * Recovers user's password, first step of KYC recovery
 */
class RecoverPasswordUseCase(
    private val email: String,
    private val newPassword: CharArray,
    private val apiProvider: ApiProvider
) {
    private lateinit var newAccount: Account

    fun perform(): Completable {
        return getNewAccount()
            .doOnSuccess { newAccount ->
                this.newAccount = newAccount
            }
            .flatMap {
                recoverPassword()
            }
            .ignoreElement()
    }

    private fun getNewAccount(): Single<Account> {
        return Account.randomSingle()
    }

    private fun recoverPassword(): Single<WalletCreateResult> {
        return KeyServer(apiProvider.getApi().wallets)
            .recoverWalletPassword(
                email,
                newPassword,
                newAccount
            )
            .toSingle()
    }
}