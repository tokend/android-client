package org.tokend.template.base.activities

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import org.tokend.template.base.logic.WalletPasswordManager
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.persistance.CredentialsPersistor
import org.tokend.wallet.Account

class ChangePasswordUseCase(
        private val newPassword: CharArray,
        private val walletPasswordManager: WalletPasswordManager,
        private val apiProvider: ApiProvider,
        private val accountProvider: AccountProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val credentialsPersistor: CredentialsPersistor?
) {
    private lateinit var newAccount: Account

    fun perform(): Completable {
        return generateNewAccount()
                .doOnSuccess { newAccount ->
                    this.newAccount = newAccount
                }
                .flatMap {
                    updateWallet()
                }
                .toCompletable()
    }

    private fun generateNewAccount(): Single<Account> {
        return {
            Account.random()
        }.toSingle()
    }

    private fun updateWallet(): Single<Boolean> {
        return walletPasswordManager.updateWalletWithNewPassword(
                apiProvider,
                accountProvider,
                walletInfoProvider,
                credentialsPersistor,
                newAccount,
                newPassword
        )
                .toSingleDefault(true)
    }
}