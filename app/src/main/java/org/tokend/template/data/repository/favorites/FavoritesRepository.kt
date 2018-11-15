package org.tokend.template.data.repository.favorites

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.sdk.api.favorites.model.FavoriteEntry
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.data.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.extensions.toCompletable
import org.tokend.template.extensions.toSingle

class FavoritesRepository(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider
) : SimpleMultipleItemsRepository<FavoriteEntry>() {
    override val itemsCache = FavoritesCache()

    override fun getItems(): Single<List<FavoriteEntry>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))

        return signedApi
                .favorites
                .get(accountId)
                .toSingle()
    }

    fun addToFavorites(entry: FavoriteEntry): Completable {
        val signedApi = apiProvider.getSignedApi()
                ?: return Completable.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Completable.error(IllegalStateException("No wallet info found"))

        return signedApi
                .favorites
                .add(accountId, entry)
                .toCompletable()
                .doOnSubscribe { isLoading = true }
                .doOnComplete {
                    isLoading = false
                    invalidate()
                }
                .doOnDispose { isLoading = false }
                .andThen(updateDeferred())
    }

    fun removeFromFavorites(entryId: Long): Completable {
        val signedApi = apiProvider.getSignedApi()
                ?: return Completable.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Completable.error(IllegalStateException("No wallet info found"))

        return signedApi
                .favorites
                .delete(accountId, entryId)
                .toCompletable()
                .doOnComplete {
                    itemsCache.items.find { it.id == entryId }?.also {
                        itemsCache.delete(it)
                        broadcast()
                    }
                }
                .doOnSubscribe { isLoading = true }
                .doOnDispose { isLoading = false }
                .doOnTerminate { isLoading = false }
    }
}