package org.tokend.template.features.deposit.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.rxkotlin.toSingle
import org.tokend.template.data.model.AccountRecord
import org.tokend.template.data.repository.AccountRepository
import org.tokend.template.data.repository.BalancesRepository
import org.tokend.template.di.providers.WalletInfoProvider

/**
 * Binds deposit account to user's account. Creates balance if needed
 *
 * Updates following repositories: account and balances if new balance was created
 */
abstract class BindDepositAccountUseCase(
        protected val asset: String,
        protected val walletInfoProvider: WalletInfoProvider,
        protected val balancesRepository: BalancesRepository,
        protected val accountRepository: AccountRepository
) {
    /**
     * Thrown when currently there are no addresses available for binding
     */
    class NoAvailableDepositAccountException : Exception()

    protected lateinit var accountId: String
    protected var isBalanceCreationRequired: Boolean = false
    protected lateinit var newDepositAccount: AccountRecord.DepositAccount

    fun perform(): Completable {
        return updateData()
                .flatMap {
                    getAccountId()
                }
                .doOnSuccess { account ->
                    this.accountId = account
                }
                .flatMap {
                    getIsBalanceCreationRequired()
                }
                .doOnSuccess { isBalanceCreationRequired ->
                    this.isBalanceCreationRequired = isBalanceCreationRequired
                }
                .flatMap {
                    getBoundDepositAccount()
                }
                .doOnSuccess { newDepositAccount ->
                    this.newDepositAccount = newDepositAccount
                }
                .doOnSuccess {
                    updateRepositories()
                }
                .ignoreElement()
    }

    private fun updateData(): Single<Boolean> {
        return balancesRepository
                .updateIfNotFreshDeferred()
                .toSingleDefault(true)
    }

    private fun getAccountId(): Single<String> {
        return walletInfoProvider
                .getWalletInfo()
                ?.accountId
                .toMaybe()
                .switchIfEmpty(Single.error(IllegalStateException("Missing account ID")))
    }

    private fun getIsBalanceCreationRequired(): Single<Boolean> {
        return balancesRepository
                .itemsList
                .find { it.assetCode == asset }
                .let { it == null }
                .toSingle()
    }

    private fun updateRepositories() {
        if (isBalanceCreationRequired) {
            balancesRepository.updateIfEverUpdated()
        }

        accountRepository.addNewDepositAccount(newDepositAccount)
    }

    protected abstract fun getBoundDepositAccount(): Single<AccountRecord.DepositAccount>
}