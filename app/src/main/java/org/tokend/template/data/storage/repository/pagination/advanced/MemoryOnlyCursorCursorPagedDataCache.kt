package org.tokend.template.data.storage.repository.pagination.advanced

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder

class MemoryOnlyCursorCursorPagedDataCache<T : CursorPagingRecord> : CursorPagedDataCache<T> {
    private val items = mutableListOf<T>()

    override fun getPage(limit: Int, cursor: Long?, order: PagingOrder): Single<DataPage<T>> {
        val actualCursor = cursor ?: if (order == PagingOrder.DESC) Long.MAX_VALUE else 0

        val pageItems = items
            .asSequence()
            .run {
                if (order == PagingOrder.DESC)
                    sortedByDescending(CursorPagingRecord::pagingCursor)
                else
                    sortedBy(CursorPagingRecord::pagingCursor)
            }
            .run {
                if (order == PagingOrder.DESC)
                    filter { it.pagingCursor < actualCursor }
                else
                    filter { it.pagingCursor > actualCursor }
            }
            .toList()
            .run {
                slice(0 until kotlin.math.min(limit, size))
            }

        return DataPage(
            nextCursor = pageItems.lastOrNull()?.pagingCursor?.toString()
                ?: cursor?.toString(),
            items = pageItems,
            isLast = pageItems.size < limit
        ).toSingle()
    }

    override fun cachePage(page: DataPage<T>) {
        items.addAll(page.items)
    }

    override fun update(vararg items: T) {
        this.items.forEachIndexed { index, item ->
            val updateIndex = items.indexOf(item)
            if (updateIndex >= 0) {
                this.items[index] = items[updateIndex]
            }
        }
    }

    override fun delete(vararg items: T) {
        this.items.removeAll(items)
    }

    override fun clear() {
        items.clear()
    }
}