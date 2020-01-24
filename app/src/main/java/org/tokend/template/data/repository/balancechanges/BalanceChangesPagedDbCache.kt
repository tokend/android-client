package org.tokend.template.data.repository.balancechanges

import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingParamsHolder
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.BalanceChangeDbEntity
import org.tokend.template.data.repository.base.pagination.PagedDbDataCache

class BalanceChangesPagedDbCache(
        private val dao: BalanceChangesDao
) : PagedDbDataCache<BalanceChange>() {
    override fun getPageFromDb(pagingParams: PagingParamsHolder): DataPage<BalanceChange> {
        val count = pagingParams.limit ?: DEFAULT_LIMIT
        val cursor = pagingParams.cursor?.toLongOrNull() ?: DEFAULT_CURSOR
        return dao.selectPageDesc(count, cursor)
                .let { entities ->
                    val lastEntityId = entities.lastOrNull()?.id
                    if (lastEntityId != null)
                        DataPage(
                                nextCursor = lastEntityId,
                                items = entities.map(BalanceChangeDbEntity::toRecord),
                                isLast = entities.size < count
                        )
                    else
                        DataPage(pagingParams.cursor, emptyList(), true)
                }
    }

    override fun cachePageToDb(page: DataPage<BalanceChange>) =
            dao.insert(*page.items.map(BalanceChangeDbEntity.Companion::fromRecord).toTypedArray())

    override fun clearDb() =
            dao.deleteAll()

    private companion object {
        private const val DEFAULT_LIMIT = 30
        private const val DEFAULT_CURSOR = Long.MAX_VALUE
    }
}