package org.tokend.template.data.repository.base.pagination

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingParamsHolder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class PagedDbDataCache<T : Any> : PagedDataCache<T> {
    protected open val executor: ExecutorService = Executors.newFixedThreadPool(5)

    override fun getPage(pagingParams: PagingParamsHolder):
            Single<DataPage<T>> = synchronized(this) {
        Single.defer { getPageFromDb(pagingParams).toSingle() }
                .subscribeOn(Schedulers.io())
    }

    override fun cachePage(page: DataPage<T>) {
        executor.submit {
            synchronized(this) {
                cachePageToDb(page)
            }
        }
    }

    override fun clear() {
        executor.submit {
            synchronized(this, this::clearDb)
        }
    }

    protected abstract fun getPageFromDb(pagingParams: PagingParamsHolder): DataPage<T>

    protected abstract fun cachePageToDb(page: DataPage<T>)

    protected abstract fun clearDb()
}