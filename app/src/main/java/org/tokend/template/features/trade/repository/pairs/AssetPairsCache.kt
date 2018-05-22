package org.tokend.template.features.trade.repository.pairs

import io.reactivex.Single
import org.tokend.sdk.api.models.AssetPair
import org.tokend.template.base.logic.repository.base.RepositoryCache

class AssetPairsCache: RepositoryCache<AssetPair>() {
    override fun isContentSame(first: AssetPair, second: AssetPair): Boolean {
        return first == second
    }

    override fun getAllFromDb(): Single<List<AssetPair>> = Single.just(emptyList())

    override fun addToDb(items: List<AssetPair>) {}

    override fun updateInDb(items: List<AssetPair>) {}

    override fun deleteFromDb(items: List<AssetPair>) {}

    override fun clearDb() {}
}