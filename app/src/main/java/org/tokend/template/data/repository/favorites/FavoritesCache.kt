package org.tokend.template.data.repository.favorites

import io.reactivex.Single
import org.tokend.template.data.model.FavoriteRecord
import org.tokend.template.data.repository.base.RepositoryCache

class FavoritesCache : RepositoryCache<FavoriteRecord>() {
    override fun isContentSame(first: FavoriteRecord, second: FavoriteRecord): Boolean {
        return first == second
    }

    override fun getAllFromDb(): Single<List<FavoriteRecord>> = Single.just(emptyList())

    override fun addToDb(items: List<FavoriteRecord>) {

    }

    override fun updateInDb(items: List<FavoriteRecord>) {

    }

    override fun deleteFromDb(items: List<FavoriteRecord>) {

    }

    override fun clearDb() {

    }
}