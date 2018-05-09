package org.tokend.template.base.logic.repository

import io.reactivex.Single
import org.tokend.sdk.api.models.transactions.Transaction
import org.tokend.template.base.logic.repository.base.RepositoryCache

class TxCache : RepositoryCache<Transaction>() {
    override fun isContentSame(first: Transaction, second: Transaction): Boolean {
        return first.id == second.id && first.pagingToken == second.pagingToken
                && first.state == second.state
    }

    override fun sortItems() {
        mItems.sortByDescending { it.date }
    }

    override fun getAllFromDb(): Single<List<Transaction>> = Single.just(emptyList())

    override fun addToDb(items: List<Transaction>) {}

    override fun updateInDb(items: List<Transaction>) {}

    override fun deleteFromDb(items: List<Transaction>) {}

    override fun clearDb() {}
}