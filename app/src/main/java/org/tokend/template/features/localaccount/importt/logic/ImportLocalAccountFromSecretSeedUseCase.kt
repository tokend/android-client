package org.tokend.template.features.localaccount.importt.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.repository.LocalAccountRepository

class ImportLocalAccountFromSecretSeedUseCase(
        private val secretSeed: CharArray,
        localAccountRepository: LocalAccountRepository
) : ImportLocalAccountUseCase(localAccountRepository) {
    override fun getLocalAccount(): Single<LocalAccount> {
        return {
            LocalAccount.fromSecretSeed(secretSeed)
        }.toSingle().subscribeOn(Schedulers.computation())
    }
}