package org.tokend.template.data.repository.assets

import io.reactivex.Single
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.extensions.Asset

class AssetsRepositoryCache : RepositoryCache<Asset>() {
    override fun isContentSame(first: Asset, second: Asset): Boolean {
        return first.code == second.code
                && first.policy == second.policy
                && first.issued == second.issued
                && first.details == second.details
    }

    override fun getAllFromDb(): Single<List<Asset>> = Single.just(emptyList())

    override fun addToDb(items: List<Asset>) {}

    override fun updateInDb(items: List<Asset>) {}

    override fun deleteFromDb(items: List<Asset>) {}

    override fun clearDb() {}
}