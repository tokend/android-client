package org.tokend.template.data.repository.favorites

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.sdk.api.favorites.model.FavoriteEntry
import org.tokend.template.data.model.FavoriteRecord
import org.tokend.template.data.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.extensions.toCompletable
import org.tokend.template.extensions.toSingle

class FavoritesRepository(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider,
        itemsCache: RepositoryCache<FavoriteRecord>
) : SimpleMultipleItemsRepository<FavoriteRecord>(itemsCache) {

    override fun getItems(): Single<List<FavoriteRecord>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))

        return signedApi
                .favorites
                .get(accountId)
                .toSingle()
                .map { entries ->
                    entries.map { FavoriteRecord(it) }
                }
    }

    /**
     * Adds given entry to favorites,
     * update repository on complete
     */
    fun addToFavorites(record: FavoriteRecord): Completable {
        val signedApi = apiProvider.getSignedApi()
                ?: return Completable.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Completable.error(IllegalStateException("No wallet info found"))

        val entry = FavoriteEntry(record.type, record.key, record.id)

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

    /**
     * Removes given entry from favorites,
     * locally removes it from repository on complete
     */
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