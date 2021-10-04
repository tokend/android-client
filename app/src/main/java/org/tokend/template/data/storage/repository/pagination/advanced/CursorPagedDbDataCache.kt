package org.tokend.template.data.storage.repository.pagination.advanced

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class CursorPagedDbDataCache<T : CursorPagingRecord> : CursorPagedDataCache<T> {
    protected open val executor: ExecutorService = Executors.newSingleThreadExecutor {
        Thread(it).apply { name = "PagedDbCacheThread" }
    }

    override fun getPage(limit: Int, cursor: Long?, order: PagingOrder):
            Single<DataPage<T>> = synchronized(this) {
        Single.defer {
            val pageItems = getPageItemsFromDb(limit, cursor, order)
            val lastItem = pageItems.lastOrNull()
            val isLast = pageItems.size < limit

            val page =
                if (lastItem == null)
                    DataPage(cursor?.toString(), pageItems, true)
                else
                    DataPage(lastItem.pagingCursor.toString(), pageItems, isLast)

            Single.just(page)
        }
            .subscribeOn(Schedulers.io())
    }

    override fun cachePage(page: DataPage<T>) {
        executor.submit {
            synchronized(this) {
                cachePageToDb(page)
            }
        }
    }

    override fun update(vararg items: T) {
        executor.submit {
            synchronized(this) {
                updateInDb(items.toSet())
            }
        }
    }

    override fun delete(vararg items: T) {
        executor.submit {
            synchronized(this) {
                deleteFromDb(items.toSet())
            }
        }
    }

    override fun clear() {
        executor.submit {
            synchronized(this, this::clearDb)
        }
    }

    protected abstract fun getPageItemsFromDb(
        limit: Int,
        cursor: Long?,
        order: PagingOrder
    ): List<T>

    protected abstract fun cachePageToDb(page: DataPage<T>)

    protected abstract fun updateInDb(items: Collection<T>)

    protected abstract fun deleteFromDb(items: Collection<T>)

    protected abstract fun clearDb()
}