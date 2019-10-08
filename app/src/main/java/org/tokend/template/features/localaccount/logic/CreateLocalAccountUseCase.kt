package org.tokend.template.features.localaccount.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.repository.LocalAccountRepository
import java.security.SecureRandom

class CreateLocalAccountUseCase(
        private val localAccountRepository: LocalAccountRepository
) {
    private lateinit var entropy: ByteArray
    private lateinit var localAccount: LocalAccount

    fun perform(): Single<LocalAccount> {
        return getEntropy()
                .doOnSuccess { entropy ->
                    this.entropy = entropy
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

    private fun getEntropy(): Single<ByteArray> {
        return {
            SecureRandom.getSeed(LocalAccount.DEFAULT_ENTROPY_BYTES)
        }.toSingle().subscribeOn(Schedulers.computation())
    }

    private fun getLocalAccount(): Single<LocalAccount> {
        return {
            LocalAccount.fromEntropy(entropy)
        }.toSingle().subscribeOn(Schedulers.computation())
    }

    private fun updateRepositories() {
        localAccountRepository.useAccount(localAccount)
    }
}