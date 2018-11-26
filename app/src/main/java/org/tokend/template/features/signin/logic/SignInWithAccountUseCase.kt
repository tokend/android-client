package org.tokend.template.features.signin.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.authenticator.AuthResultsApi
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.wallet.Account

/**
 * Performs sign in with given [Account]
 *
 * @param repositoryProvider if set then [SignInManager.doPostSignIn] will be performed
 */
class SignInWithAccountUseCase(
        private val account: Account,
        private val signInManager: SignInManager,
        private val authResultsApi: AuthResultsApi,
        private val repositoryProvider: RepositoryProvider?
) {
    fun perform(): Completable {
        val scheduler = Schedulers.newThread()

        return performSignIn()
                .observeOn(scheduler)
                .flatMap {
                    performPostSignIn()
                }
                .observeOn(scheduler)
                .toCompletable()
    }

    private fun performSignIn(): Single<Boolean> {
        return signInManager.signIn(account, authResultsApi)
                .toSingleDefault(true)
    }

    private fun performPostSignIn(): Single<Boolean> {
        return if (repositoryProvider != null)
            signInManager
                    .doPostSignIn(repositoryProvider)
                    .toSingleDefault(true)
        else
            Single.just(false)
    }
}