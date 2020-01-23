package org.tokend.template.data.repository.balancechanges

import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.BalanceChangeDbEntity
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.extensions.mapSuccessful

class BalanceChangesDbCache(
        private val dao: BalanceChangesDao
) : RepositoryCache<BalanceChange>() {
    override fun isContentSame(first: BalanceChange, second: BalanceChange): Boolean {
        return false
    }

    override fun sortItems() {
        mItems.sortByDescending { it.date }
    }

    override fun getAllFromDb() =
            dao.selectAll().mapSuccessful(BalanceChangeDbEntity::toRecord)

    override fun addToDb(items: Collection<BalanceChange>) =
            dao.insert(*items.map(BalanceChangeDbEntity.Companion::fromRecord).toTypedArray())

    override fun updateInDb(items: Collection<BalanceChange>) =
            dao.update(*items.map(BalanceChangeDbEntity.Companion::fromRecord).toTypedArray())

    override fun deleteFromDb(items: Collection<BalanceChange>) =
            dao.delete(*items.map(BalanceChangeDbEntity.Companion::fromRecord).toTypedArray())

    override fun clearDb() =
            dao.deleteAll()
}