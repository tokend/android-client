package io.tokend.template.features.localaccount.importt.logic

import io.reactivex.Single
import io.tokend.template.features.localaccount.model.LocalAccount
import io.tokend.template.features.localaccount.storage.LocalAccountRepository
import io.tokend.template.features.userkey.logic.UserKeyProvider
import java.util.concurrent.CancellationException

abstract class ImportLocalAccountUseCase(
    protected val userKeyProvider: UserKeyProvider,
    protected val localAccountRepository: LocalAccountRepository
) {
    protected lateinit var localAccount: LocalAccount

    open fun perform(): Single<LocalAccount> {
        return getLocalAccount()
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