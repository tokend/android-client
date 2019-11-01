package org.tokend.template.features.localaccount.importt.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.repository.LocalAccountRepository
import org.tokend.template.features.userkey.logic.UserKeyProvider
import org.tokend.template.util.cipher.DataCipher

class ImportLocalAccountFromSecretSeedUseCase(
        private val secretSeed: CharArray,
        private val cipher: DataCipher,
        userKeyProvider: UserKeyProvider,
        localAccountRepository: LocalAccountRepository
) : ImportLocalAccountUseCase(userKeyProvider, localAccountRepository) {
    private lateinit var userKey: CharArray

    override fun getLocalAccount(): Single<LocalAccount> {
        return getUserKey()
                .doOnSuccess { userKey ->
                    this.userKey = userKey
                }
                .flatMap {
                    getEncryptedLocalAccount()
                }
    }

    private fun getEncryptedLocalAccount(): Single<LocalAccount> {
        return {
            LocalAccount.fromSecretSeed(secretSeed, cipher, userKey)
        }.toSingle().subscribeOn(Schedulers.computation())
    }
}