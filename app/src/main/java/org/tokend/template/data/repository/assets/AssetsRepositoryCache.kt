package org.tokend.template.data.repository.assets

import io.reactivex.Single
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.features.assets.model.AssetRecord

class AssetsRepositoryCache : RepositoryCache<AssetRecord>() {
    override fun isContentSame(first: AssetRecord, second: AssetRecord): Boolean {
        return first.code == second.code
                && first.policy == second.policy
                && first.issued == second.issued
                && first.name == second.name
                && first.logoUrl == second.logoUrl
                && first.terms == second.terms
    }

    override fun getAllFromDb(): Single<List<AssetRecord>> = Single.just(emptyList())

    override fun addToDb(items: List<AssetRecord>) {}

    override fun updateInDb(items: List<AssetRecord>) {}

    override fun deleteFromDb(items: List<AssetRecord>) {}

    override fun clearDb() {}
}