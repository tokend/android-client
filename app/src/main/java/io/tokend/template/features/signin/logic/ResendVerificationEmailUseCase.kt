package io.tokend.template.features.signin.logic

import io.reactivex.Completable
import org.tokend.rx.extensions.toCompletable
import org.tokend.sdk.api.TokenDApi

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