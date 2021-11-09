package io.tokend.template.features.localaccount.logic

import io.reactivex.Maybe
import io.reactivex.Single
import io.tokend.template.features.localaccount.model.LocalAccount
import io.tokend.template.features.userkey.logic.UserKeyProvider
import io.tokend.template.util.cipher.DataCipher
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Decrypts local accounts with retries on invalid key
 */
class LocalAccountRetryDecryptor(
    private val userKeyProvider: UserKeyProvider,
    private val cipher: DataCipher,
    private val visualDelay: Long = VISUAL_DELAY_MS
) {
    fun decrypt(localAccount: LocalAccount): Single<LocalAccount> {
        if (localAccount.isDecrypted) {
            return Single.just(localAccount)
        }

        var isRetry = false

        val getUserKeyDeferred = Maybe.defer {
            userKeyProvider.getUserKey(isRetry)
        }

        return getUserKeyDeferred
            .switchIfEmpty(Single.error(CancellationException()))
            .delay(visualDelay, TimeUnit.MILLISECONDS)
            .flatMap { userKey ->
                try {
                    localAccount.decrypt(cipher, userKey)
                    Single.just(localAccount)
                } catch (e: Exception) {
                    isRetry = true
                    Single.error(e)
                }
            }
            .retry { error ->
                error !is CancellationException
            }
    }

    companion object {
        const val VISUAL_DELAY_MS = 1000L
    }
}