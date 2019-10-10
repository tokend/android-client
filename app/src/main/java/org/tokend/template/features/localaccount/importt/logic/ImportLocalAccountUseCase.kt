package org.tokend.template.features.localaccount.importt.logic

import io.reactivex.Single
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.repository.LocalAccountRepository

abstract class ImportLocalAccountUseCase(
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

    abstract fun getLocalAccount(): Single<LocalAccount>

    protected open fun updateRepositories() {
        localAccountRepository.useAccount(localAccount)
    }
}