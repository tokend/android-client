package org.tokend.template.data.repository.balances

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.repository.base.RepositoryCache

class BalancesCache : RepositoryCache<BalanceRecord>() {
    override fun isContentSame(first: BalanceRecord, second: BalanceRecord): Boolean {
        return false
    }

    override fun getAllFromDb(): Single<List<BalanceRecord>> =
            listOf<BalanceRecord>().toSingle()

    override fun addToDb(items: List<BalanceRecord>) {}

    override fun updateInDb(items: List<BalanceRecord>) {}

    override fun deleteFromDb(items: List<BalanceRecord>) {}

    override fun clearDb() {}
}