package org.tokend.template.features.invest.repository

import io.reactivex.Single
import org.tokend.template.base.logic.repository.base.RepositoryCache
import org.tokend.sdk.api.models.sale.SimpleSale

class SalesCache : RepositoryCache<SimpleSale>() {
    override fun isContentSame(first: SimpleSale, second: SimpleSale): Boolean {
        return first == second
    }

    override fun getAllFromDb(): Single<List<SimpleSale>> = Single.just(emptyList())

    override fun addToDb(items: List<SimpleSale>) {}

    override fun updateInDb(items: List<SimpleSale>) {}

    override fun deleteFromDb(items: List<SimpleSale>) {}

    override fun clearDb() {}
}