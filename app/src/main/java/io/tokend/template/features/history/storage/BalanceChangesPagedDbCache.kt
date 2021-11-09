package io.tokend.template.features.history.storage

import io.tokend.template.data.storage.repository.pagination.advanced.CursorPagedDbDataCache
import io.tokend.template.features.history.model.BalanceChange
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder

class BalanceChangesPagedDbCache(
    private val balanceId: String?,
    private val dao: BalanceChangesDao
) : CursorPagedDbDataCache<BalanceChange>() {
    override fun getPageItemsFromDb(
        limit: Int,
        cursor: Long?,
        order: PagingOrder
    ): List<BalanceChange> {
        val actualCursor = cursor ?: Long.MAX_VALUE

        if (order != PagingOrder.DESC) {
            throw NotImplementedError("Only DESC pagination is supported")
        }

        val entities =
            if (balanceId == null)
                dao.selectPageDesc(limit, actualCursor)
            else
                dao.selectPageByBalanceIdDesc(balanceId, limit, actualCursor)

        return entities.map(BalanceChangeDbEntity::toRecord)
    }

    override fun cachePageToDb(page: DataPage<BalanceChange>) =
        dao.insert(*page.items.map(BalanceChangeDbEntity.Companion::fromRecord).toTypedArray())

    override fun updateInDb(items: Collection<BalanceChange>) =
        dao.update(*items.map(BalanceChangeDbEntity.Companion::fromRecord).toTypedArray())

    override fun deleteFromDb(items: Collection<BalanceChange>) =
        dao.delete(*items.map(BalanceChangeDbEntity.Companion::fromRecord).toTypedArray())

    override fun clearDb() =
        dao.deleteAll()
}