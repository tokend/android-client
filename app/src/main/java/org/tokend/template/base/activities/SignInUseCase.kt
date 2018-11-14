package org.tokend.template.base.activities

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.template.base.logic.SignInManager
import org.tokend.template.base.logic.di.providers.RepositoryProvider

/**
 * Performs sign in with given credentials
 *
 * @param repositoryProvider if set then [SignInManager.doPostSignIn] will be performed
 */
class SignInUseCase(
        private val email: String,
        private val password: CharArray,
        private val signInManager: SignInManager,
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
                .retry { attempt, error ->
                    error is SignInManager.InvalidPersistedCredentialsException && attempt == 1
                }
                .toCompletable()
    }

    private fun performSignIn(): Single<Boolean> {
        return signInManager.signIn(email, password)
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