package org.tokend.template.features.signin.logic

import io.reactivex.Completable
import io.reactivex.rxkotlin.subscribeBy
import org.tokend.template.di.providers.RepositoryProvider
import retrofit2.HttpException
import java.net.HttpURLConnection

class PostSignInManager(
        private val repositoryProvider: RepositoryProvider
) {
    class AuthMismatchException : Exception()

    /**
     * Updates all important repositories.
     */
    fun doPostSignIn(): Completable {
        val parallelActions = listOf<Completable>(
                // Added actions will be performed simultaneously.
                repositoryProvider.balances().updateDeferred()
        )
        val syncActions = listOf<Completable>(
                // Added actions will be performed on after another in
                // provided order.
                repositoryProvider.tfaFactors().updateDeferred()
                        .onErrorResumeNext {
                            if (it is HttpException
                                    && it.code() == HttpURLConnection.HTTP_NOT_FOUND)
                                Completable.error(
                                        AuthMismatchException()
                                )
                            else
                                Completable.error(it)
                        }
        )

        val performParallelActions = Completable.merge(parallelActions)
        val performSyncActions = Completable.concat(syncActions)

        repositoryProvider.kycState().update().subscribeBy(onError = {
            it.printStackTrace()
        })

        repositoryProvider.tfaFactors().invalidate()

        return performSyncActions
                .andThen(performParallelActions)
    }
}