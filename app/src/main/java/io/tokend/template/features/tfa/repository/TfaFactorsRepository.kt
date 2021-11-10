package io.tokend.template.features.tfa.repository

import io.reactivex.Completable
import io.reactivex.Single
import io.tokend.template.data.storage.repository.MultipleItemsRepository
import io.tokend.template.data.storage.repository.RepositoryCache
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import io.tokend.template.features.tfa.model.TfaFactorCreationResult
import io.tokend.template.features.tfa.model.TfaFactorRecord
import org.tokend.rx.extensions.toCompletable
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.tfa.model.TfaFactor

class TfaFactorsRepository(
    private val apiProvider: ApiProvider,
    private val walletInfoProvider: WalletInfoProvider,
    itemsCache: RepositoryCache<TfaFactorRecord>
) : MultipleItemsRepository<TfaFactorRecord>(itemsCache) {

    override fun getItems(): Single<List<TfaFactorRecord>> {
        val signedApi = apiProvider.getSignedApi()
        val walletId = walletInfoProvider.getWalletInfo().walletId

        return signedApi
            .tfa
            .getFactors(walletId)
            .toSingle()
            .map { factors ->
                factors.map {
                    TfaFactorRecord(it)
                }
            }
    }

    /**
     * Adds given factor as disabled,
     * locally adds it to repository on complete
     */
    fun addFactor(type: TfaFactor.Type): Single<TfaFactorCreationResult> {
        val signedApi = apiProvider.getSignedApi()
        val walletId = walletInfoProvider.getWalletInfo().walletId

        return signedApi
            .tfa
            .createFactor(walletId, type)
            .toSingle()
            .map {
                TfaFactorCreationResult(
                    TfaFactorRecord(it.newFactor),
                    it.confirmationAttributes
                )
            }
            .doOnSuccess { (newFactor, _) ->
                itemsCache.add(newFactor)
                broadcast()
            }
            .doOnSubscribe { isLoading = true }
            .doOnDispose { isLoading = false }
            .doOnEvent { _, _ -> isLoading = false }
    }

    /**
     * Updates priority of factor with given id to the maximum one,
     * locally updates factor in repository on complete
     */
    fun setFactorAsMain(id: Long): Completable {
        val signedApi = apiProvider.getSignedApi()
        val walletId = walletInfoProvider.getWalletInfo().walletId

        var newPriority = 0

        return updateIfNotFreshDeferred()
            // Obtain max priority.
            .andThen(
                Single.just(
                    itemsCache.items.maxByOrNull {
                        it.priority
                    }
                        ?.priority
                        ?: 0
                )
            )
            // Set it for given factor.
            .flatMapCompletable { maxPriority ->
                newPriority = maxPriority + 1
                signedApi
                    .tfa
                    .updateFactor(
                        walletId,
                        id,
                        TfaFactor.Attributes(priority = newPriority)
                    )
                    .toCompletable()
            }
            // Update factor in local cache
            .doOnComplete {
                itemsCache.items.find { it.id == id }?.let { updatedFactor ->
                    updatedFactor.priority = newPriority

                    itemsCache.transform(listOf(updatedFactor)) { it.id == id }
                    broadcast()
                }
            }
            .doOnSubscribe { isLoading = true }
            .doOnDispose { isLoading = false }
            .doOnTerminate { isLoading = false }
    }

    /**
     * Deletes factor with given id,
     * locally deletes it from repository on complete
     */
    fun deleteFactor(id: Long): Completable {
        val signedApi = apiProvider.getSignedApi()
        val walletId = walletInfoProvider.getWalletInfo().walletId

        return signedApi
            .tfa
            .deleteFactor(walletId, id)
            .toCompletable()
            .doOnComplete {
                itemsCache.transform(emptyList()) { it.id == id }
                broadcast()
            }
            .doOnSubscribe { isLoading = true }
            .doOnDispose { isLoading = false }
            .doOnTerminate { isLoading = false }
    }
}