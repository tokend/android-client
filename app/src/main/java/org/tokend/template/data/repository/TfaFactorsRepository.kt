package org.tokend.template.data.repository

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.rx.extensions.toCompletable
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.MultipleItemsRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.features.tfa.model.TfaFactorCreationResult
import org.tokend.template.features.tfa.model.TfaFactorRecord

class TfaFactorsRepository(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider,
        itemsCache: RepositoryCache<TfaFactorRecord>
) : MultipleItemsRepository<TfaFactorRecord>(itemsCache) {

    override fun getItems(): Single<List<TfaFactorRecord>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val walletId = walletInfoProvider.getWalletInfo()?.walletIdHex
                ?: return Single.error(IllegalStateException("No wallet info found"))

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
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val walletId = walletInfoProvider.getWalletInfo()?.walletIdHex
                ?: return Single.error(IllegalStateException("No wallet info found"))

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
                ?: return Completable.error(IllegalStateException("No signed API instance found"))
        val walletId = walletInfoProvider.getWalletInfo()?.walletIdHex
                ?: return Completable.error(IllegalStateException("No wallet info found"))

        var newPriority = 0

        return updateIfNotFreshDeferred()
                // Obtain max priority.
                .andThen(
                        Single.just(
                                itemsCache.items.maxBy {
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
                ?: return Completable.error(IllegalStateException("No signed API instance found"))
        val walletId = walletInfoProvider.getWalletInfo()?.walletIdHex
                ?: return Completable.error(IllegalStateException("No wallet info found"))

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