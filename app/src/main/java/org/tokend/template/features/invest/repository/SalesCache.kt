package org.tokend.template.features.invest.repository

import io.reactivex.Single
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.extensions.Sale

class SalesCache : RepositoryCache<Sale>() {
    override fun isContentSame(first: Sale, second: Sale): Boolean {
        return first.currentCap == second.currentCap
                && first.baseCurrentCap == second.baseCurrentCap
                && first.statistics == second.statistics
                && first.state == second.state
                && first.details == second.details
    }

    override fun getAllFromDb(): Single<List<Sale>> = Single.just(emptyList())

    override fun addToDb(items: List<Sale>) {}

    override fun updateInDb(items: List<Sale>) {}

    override fun deleteFromDb(items: List<Sale>) {}

    override fun clearDb() {}
}