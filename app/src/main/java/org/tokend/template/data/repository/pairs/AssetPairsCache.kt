package org.tokend.template.data.repository.pairs

import io.reactivex.Single
import org.tokend.template.data.model.AssetPairRecord
import org.tokend.template.data.repository.base.RepositoryCache

class AssetPairsCache : RepositoryCache<AssetPairRecord>() {
    override fun isContentSame(first: AssetPairRecord, second: AssetPairRecord): Boolean {
        return first == second
    }

    override fun getAllFromDb(): Single<List<AssetPairRecord>> = Single.just(emptyList())

    override fun addToDb(items: List<AssetPairRecord>) {}

    override fun updateInDb(items: List<AssetPairRecord>) {}

    override fun deleteFromDb(items: List<AssetPairRecord>) {}

    override fun clearDb() {}
}