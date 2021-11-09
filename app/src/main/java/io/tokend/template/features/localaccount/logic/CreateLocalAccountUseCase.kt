package io.tokend.template.features.localaccount.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import io.tokend.template.features.localaccount.model.LocalAccount
import io.tokend.template.features.localaccount.storage.LocalAccountRepository
import io.tokend.template.features.userkey.logic.UserKeyProvider
import io.tokend.template.util.cipher.DataCipher
import java.security.SecureRandom
import java.util.concurrent.CancellationException

class CreateLocalAccountUseCase(
    private val cipher: DataCipher,
    private val userKeyProvider: UserKeyProvider,
    private val localAccountRepository: LocalAccountRepository
) {
    private lateinit var entropy: ByteArray
    private lateinit var userKey: CharArray
    private lateinit var localAccount: LocalAccount

    fun perform(): Single<LocalAccount> {
        return getEntropy()
            .doOnSuccess { entropy ->
                this.entropy = entropy
            }
            .flatMap {
                getUserKey()
            }
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

    private fun getEntropy(): Single<ByteArray> {
        return {
            SecureRandom.getSeed(LocalAccount.DEFAULT_ENTROPY_BYTES)
        }.toSingle().subscribeOn(Schedulers.computation())
    }

    private fun getUserKey(): Single<CharArray> {
        return userKeyProvider
            .getUserKey(isRetry = false)
            .switchIfEmpty(Single.error(CancellationException()))
    }

    private fun getLocalAccount(): Single<LocalAccount> {
        return {
            LocalAccount.fromEntropy(entropy, cipher, userKey)
        }.toSingle().subscribeOn(Schedulers.computation())
    }

    private fun updateRepositories() {
        localAccountRepository.useAccount(localAccount)
    }
}