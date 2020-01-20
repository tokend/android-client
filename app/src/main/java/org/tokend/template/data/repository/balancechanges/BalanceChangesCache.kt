package org.tokend.template.data.repository.balancechanges

import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.repository.base.RepositoryCache

class BalanceChangesCache: RepositoryCache<BalanceChange>() {
    override fun isContentSame(first: BalanceChange, second: BalanceChange): Boolean {
        return false
    }

    override fun sortItems() {
        mItems.sortByDescending { it.date }
    }

    override fun getAllFromDb() = emptyList<BalanceChange>()

    override fun addToDb(items: Collection<BalanceChange>) {}

    override fun updateInDb(items: Collection<BalanceChange>) {}

    override fun deleteFromDb(items: Collection<BalanceChange>) {}

    override fun clearDb() {}
}