package org.tokend.template.features.invest.repository

import io.reactivex.Single
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.features.invest.model.SaleRecord

class SalesCache : RepositoryCache<SaleRecord>() {
    override fun isContentSame(first: SaleRecord, second: SaleRecord): Boolean {
        return false
    }

    override fun getAllFromDb(): Single<List<SaleRecord>> = Single.just(emptyList())

    override fun addToDb(items: List<SaleRecord>) {}

    override fun updateInDb(items: List<SaleRecord>) {}

    override fun deleteFromDb(items: List<SaleRecord>) {}

    override fun clearDb() {}
}