package org.tokend.template.base.logic.repository.tfa

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.extensions.toCompletable
import org.tokend.template.extensions.toSingle

class TfaBackendsRepository(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider
) : SimpleMultipleItemsRepository<TfaFactor>() {
    override val itemsCache = TfaBackendsCache()

    override fun getItems(): Single<List<TfaFactor>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val walletId = walletInfoProvider.getWalletInfo()?.walletIdHex
                ?: return Single.error(IllegalStateException("No wallet info found"))

        return signedApi
                .tfa
                .getFactors(walletId)
                .toSingle()
    }

    fun addBackend(type: TfaFactor.Type): Single<TfaFactor> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val walletId = walletInfoProvider.getWalletInfo()?.walletIdHex
                ?: return Single.error(IllegalStateException("No wallet info found"))

        return signedApi
                .tfa
                .createFactor(walletId, type)
                .toSingle()
                .doOnSuccess {
                    itemsCache.add(it)
                    broadcast()
                }
                .doOnSubscribe { isLoading = true }
                .doOnDispose { isLoading = false }
                .doOnEvent { _, _ -> isLoading = false }
    }

    fun setBackendAsMain(id: Long): Completable {
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
                                    it.attributes?.priority ?: 0
                                }
                                        ?.attributes?.priority
                                        ?: 0
                        )
                )
                // Set it for given backend.
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
                // Update backend in local cache
                .doOnComplete {
                    itemsCache.items.find { it.id == id }?.let { updatedBackend ->
                        updatedBackend.attributes.priority = newPriority

                        itemsCache.transform(listOf(updatedBackend)) { it.id == id }
                        broadcast()
                    }
                }
                .doOnSubscribe { isLoading = true }
                .doOnDispose { isLoading = false }
                .doOnTerminate { isLoading = false }
    }

    fun deleteBackend(id: Long): Completable {
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

    fun getBackendByType(type: TfaFactor.Type): Maybe<TfaFactor> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Maybe.error(IllegalStateException("No signed API instance found"))
        val walletId = walletInfoProvider.getWalletInfo()?.walletIdHex
                ?: return Maybe.error(IllegalStateException("No wallet info found"))

        return updateIfNotFreshDeferred()
                .andThen(itemsCache.items.find { it.type == type }.toMaybe())
    }
}