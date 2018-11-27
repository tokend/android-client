package org.tokend.template.features.signin.logic

import io.reactivex.Completable
import org.tokend.template.di.providers.RepositoryProvider
import retrofit2.HttpException
import java.net.HttpURLConnection

class PostSignInManager(
        private val repositoryProvider: RepositoryProvider
) {
    class AuthMismatchException : Exception()

    /**
     * Updates all important repositories, creates account on first sign in.
     */
    fun doPostSignIn(): Completable {
        val parallelActions = listOf<Completable>(
                // Added actions will be performed simultaneously.

                repositoryProvider.balances().updateDeferred(),
                repositoryProvider.tfaFactors().updateDeferred()
                        .onErrorResumeNext {
                            if (it is HttpException
                                    && it.code() == HttpURLConnection.HTTP_NOT_FOUND)
                                Completable.error(
                                        AuthMismatchException()
                                )
                            else
                                Completable.error(it)
                        },
                repositoryProvider.favorites().updateIfNotFreshDeferred()
        )
        val syncActions = listOf<Completable>(
                // Added actions will be performed on after another in
                // provided order.

                // Update account just to create user for the first time.
                repositoryProvider.account().updateDeferred()
                        .onErrorResumeNext { error ->
                            if (error is HttpException
                                    && error.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                                createUnverifiedUser(repositoryProvider)
                            } else if (error is HttpException
                                    && error.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                Completable.error(AuthMismatchException())
                            } else {
                                // Other account update errors are not critical.
                                Completable.complete()
                            }
                        }
        )

        val performParallelActions = Completable.merge(parallelActions)
        val performSyncActions = Completable.concat(syncActions)

        return performSyncActions
                .andThen(performParallelActions)
    }

    private fun createUnverifiedUser(repositoryProvider: RepositoryProvider): Completable {
        return repositoryProvider.user().createUnverified()
                .onErrorResumeNext {
                    if (it is HttpException
                            && (it.code() == HttpURLConnection.HTTP_UNAUTHORIZED
                                    || it.code() == HttpURLConnection.HTTP_FORBIDDEN)
                    )
                        Completable.error(
                                AuthMismatchException()
                        )
                    else
                        Completable.error(it)
                }
    }
}