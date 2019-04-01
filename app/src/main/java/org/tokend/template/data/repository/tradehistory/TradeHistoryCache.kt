package org.tokend.template.data.repository.tradehistory

import io.reactivex.Single
import org.tokend.template.data.model.TradeHistoryRecord
import org.tokend.template.data.repository.base.RepositoryCache

class TradeHistoryCache : RepositoryCache<TradeHistoryRecord>() {

    override fun isContentSame(first: TradeHistoryRecord, second: TradeHistoryRecord): Boolean {
        return false
    }

    override fun getAllFromDb(): Single<List<TradeHistoryRecord>> = Single.just(emptyList())

    override fun addToDb(items: List<TradeHistoryRecord>) { }

    override fun updateInDb(items: List<TradeHistoryRecord>) { }

    override fun deleteFromDb(items: List<TradeHistoryRecord>) { }

    override fun clearDb() { }
}