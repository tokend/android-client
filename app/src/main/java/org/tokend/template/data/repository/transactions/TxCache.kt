package org.tokend.template.data.repository.transactions

import io.reactivex.Single
import org.tokend.sdk.api.base.model.operations.TransferOperation
import org.tokend.template.data.repository.base.RepositoryCache

class TxCache : RepositoryCache<TransferOperation>() {
    override fun isContentSame(first: TransferOperation, second: TransferOperation): Boolean {
        return first.id == second.id && first.pagingToken == second.pagingToken
                && first.state == second.state
    }

    override fun sortItems() {
        mItems.sortByDescending { it.date }
    }

    override fun getAllFromDb(): Single<List<TransferOperation>> = Single.just(emptyList())

    override fun addToDb(items: List<TransferOperation>) {}

    override fun updateInDb(items: List<TransferOperation>) {}

    override fun deleteFromDb(items: List<TransferOperation>) {}

    override fun clearDb() {}
}