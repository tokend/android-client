package org.tokend.template.features.localaccount.importt.logic

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.repository.LocalAccountRepository
import org.tokend.template.features.userkey.logic.UserKeyProvider
import java.util.concurrent.CancellationException

abstract class ImportLocalAccountUseCase(
        protected val userKeyProvider: UserKeyProvider,
        protected val localAccountRepository: LocalAccountRepository
) {
    protected lateinit var userKey: CharArray
    protected lateinit var localAccount: LocalAccount

    open fun perform(): Single<LocalAccount> {
        return getUserKey()
                .observeOn(Schedulers.newThread())
                .doOnSuccess { userKey ->
                    this.userKey = userKey
                }
                .flatMap {
                    getLocalAccount()
                }
                .doOnSuccess { localAccount ->
                    this.localAccount = localAccount
                }
                .doOnSuccess {
                    updateRepositories()
                }
    }

    protected open fun getUserKey(): Single<CharArray> {
        return userKeyProvider
                .getUserKey(isRetry = false)
                .switchIfEmpty(Single.error(CancellationException()))
    }

    abstract fun getLocalAccount(): Single<LocalAccount>

    protected open fun updateRepositories() {
        localAccountRepository.useAccount(localAccount)
    }
}