package org.tokend.template.base.logic.repository.favorites

import io.reactivex.Single
import org.tokend.sdk.api.models.FavoriteEntry
import org.tokend.template.base.logic.repository.base.RepositoryCache

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