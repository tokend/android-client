package org.tokend.template.features.signin.logic

import io.reactivex.Completable
import io.reactivex.rxkotlin.subscribeBy
import org.tokend.sdk.utils.extentions.isUnauthorized
import org.tokend.template.di.providers.RepositoryProvider
import retrofit2.HttpException

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
                repositoryProvider.balances().ensureData(),
                repositoryProvider.account().ensureData()
        )
        val syncActions = listOf<Completable>(
                // Added actions will be performed on after another in
                // provided order.
        )

        val performParallelActions = Completable.merge(parallelActions)
        val performSyncActions = Completable.concat(syncActions)

        repositoryProvider.kycState().ensureData().subscribeBy(onError = {
            it.printStackTrace()
        })

        repositoryProvider.tfaFactors().invalidate()

        return performSyncActions
                .andThen(performParallelActions)
                .onErrorResumeNext {
                    if (it is HttpException && it.isUnauthorized())
                        Completable.error(AuthMismatchException())
                    else
                        Completable.error(it)
                }
    }
}