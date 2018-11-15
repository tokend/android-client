package org.tokend.template.features.signin.logic

import io.reactivex.Completable
import org.tokend.sdk.api.TokenDApi
import org.tokend.template.extensions.toCompletable

/**
 * Requests email verification letter resend
 *
 * @param walletId id of the related wallet
 */
class ResendVerificationEmailUseCase(
        private val walletId: String,
        private val api: TokenDApi
) {
    fun perform(): Completable {
        return api
                .wallets
                .requestVerification(walletId)
                .toCompletable()
    }
}