package io.tokend.template.features.signin.logic

import io.reactivex.Completable
import io.reactivex.rxkotlin.subscribeBy
import io.tokend.template.di.providers.RepositoryProvider
import org.tokend.sdk.utils.extentions.isUnauthorized
import retrofit2.HttpException

/**
 * Performs post sign in actions such as repositories
 * update and invalidation
 *
 * @param connectionStateProvider required to perform intelligent flow
 * in case of missing internet connection
 */
class PostSignInManager(
    private val repositoryProvider: RepositoryProvider,
    private val connectionStateProvider: (() -> Boolean)? = null
) {
    class AuthMismatchException : Exception()

    private val isOnline: Boolean
        get() = connectionStateProvider?.invoke() ?: true

    /**
     * Updates all important repositories.
     */
    fun doPostSignIn(): Completable {
        val parallelActions = listOf(
            // Added actions will be performed simultaneously.
            repositoryProvider.balances.ensureData(),

            // Actual account info is required to handle KYC recovery,
            // but we can use cached if there is no connection.
            Completable.defer {
                repositoryProvider.account.run {
                    if (isOnline)
                        updateDeferred()
                    else
                        ensureData()
                }
            }
        )
        val syncActions = listOf<Completable>(
            // Added actions will be performed on after another in
            // provided order.
        )

        val performParallelActions = Completable.merge(parallelActions)
        val performSyncActions = Completable.concat(syncActions)

        repositoryProvider.activeKyc.run {
            ensureData()
                .doOnComplete {
                    if (!isFresh) {
                        update()
                    }
                }
        }
            .subscribeBy(onError = {
                it.printStackTrace()
            })

        repositoryProvider.tfaFactors.invalidate()

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