package org.tokend.template.data.repository.base.pagination

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder

class MemoryOnlyPagedDataCache<T : PagingRecord> : PagedDataCache<T> {
    private val items = mutableListOf<T>()

    override fun getPage(limit: Int, cursor: Long?, order: PagingOrder): Single<DataPage<T>> {
        val actualCursor = cursor ?: if (order == PagingOrder.DESC) Long.MAX_VALUE else 0

        val pageItems = items
                .asSequence()
                .run {
                    if (order == PagingOrder.DESC)
                        sortedByDescending(PagingRecord::getPagingId)
                    else
                        sortedBy(PagingRecord::getPagingId)
                }
                .run {
                    if (order == PagingOrder.DESC)
                        filter { it.getPagingId() < actualCursor }
                    else
                        filter { it.getPagingId() > actualCursor }
                }
                .toList()
                .run {
                    slice(0 until kotlin.math.min(limit, size))
                }

        return DataPage(
                nextCursor = pageItems.lastOrNull()?.getPagingId()?.toString()
                        ?: cursor?.toString(),
                items = pageItems,
                isLast = pageItems.size < limit
        ).toSingle()
    }

    override fun cachePage(page: DataPage<T>) {
        items.addAll(page.items)
    }

    override fun clear() {
        items.clear()
    }
}