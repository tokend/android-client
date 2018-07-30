package org.tokend.template.base.logic.repository.base.pagination

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import org.tokend.template.base.logic.repository.base.MultipleItemsRepository

/**
 * Repository for paged data of type [T] with request params of type [R].
 */
abstract class PagedDataRepository<T, R> : MultipleItemsRepository<T>()
        where R : PagedRequestParams {
    protected var nextCursor: String = PageParams.DEFAULT_CURSOR

    val isOnFirstPage: Boolean
        get() = nextCursor == PageParams.DEFAULT_CURSOR

    var noMoreItems: Boolean = false
        protected set

    abstract fun getPage(requestParams: R): Single<DataPage<T>>
    protected abstract fun getNextPageRequestParams(): R

    protected var loadingDisposable: Disposable? = null
    protected open fun loadMore(force: Boolean): Boolean {
        synchronized(this) {
            if ((noMoreItems || isLoading) && !force) {
                return false
            }

            isLoading = true

            loadingDisposable?.dispose()
            loadingDisposable = getPage(getNextPageRequestParams())
                    .subscribeBy(
                            onSuccess = {
                                onNewItems(it.items)

                                isLoading = false
                                nextCursor = it.nextCursor ?: ""
                                noMoreItems = it.isLast
                            },
                            onError = {
                                isLoading = false
                                errorsSubject.onNext(it)
                            }
                    )
        }
        return true
    }

    override fun onNewItems(newItems: List<T>) {
        isNeverUpdated = false
        if (isOnFirstPage) {
            isFresh = true
        }

        if (isOnFirstPage) {
            itemsCache.merge(newItems)
        } else {
            itemsCache.merge(newItems, { false })
        }
        broadcast()

        if (newItems.isEmpty()) {
            noMoreItems = true
        }
    }

    open fun loadMore(): Boolean {
        return loadMore(force = false)
    }

    override fun update(): Completable {
        synchronized(this) {
            itemsCache.clear()
            nextCursor = PageParams.DEFAULT_CURSOR
            noMoreItems = false

            isLoading = false

            loadMore(force = true)
        }

        return Completable.complete()
    }
}