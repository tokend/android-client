package org.tokend.template.data.repository.favorites

import io.reactivex.Single
import org.tokend.sdk.api.favorites.model.FavoriteEntry
import org.tokend.template.data.repository.base.RepositoryCache

class FavoritesCache : RepositoryCache<FavoriteEntry>() {
    override fun isContentSame(first: FavoriteEntry, second: FavoriteEntry): Boolean {
        return first == second
    }

    override fun getAllFromDb(): Single<List<FavoriteEntry>> = Single.just(emptyList())

    override fun addToDb(items: List<FavoriteEntry>) {

    }

    override fun updateInDb(items: List<FavoriteEntry>) {

    }

    override fun deleteFromDb(items: List<FavoriteEntry>) {

    }

    override fun clearDb() {

    }
}