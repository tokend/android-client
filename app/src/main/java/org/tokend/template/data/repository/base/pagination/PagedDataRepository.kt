package org.tokend.template.data.repository.base.pagination

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.CompletableSubject
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.template.data.repository.base.MultipleItemsRepository
import org.tokend.template.data.repository.base.RepositoryCache

/**
 * Repository for paged data of type [T] with request params of type [R].
 */
abstract class PagedDataRepository<T>(itemsCache: RepositoryCache<T>)
    : MultipleItemsRepository<T>(itemsCache) {
    private var nextCursor: String? = null

    val isOnFirstPage: Boolean
        get() = nextCursor == null

    var noMoreItems: Boolean = false
        protected set

    abstract fun getPage(nextCursor: String?): Single<DataPage<T>>

    protected var loadingDisposable: Disposable? = null
    protected open fun loadMore(force: Boolean,
                                resultSubject: CompletableSubject?): Boolean {
        synchronized(this) {
            if ((noMoreItems || isLoading) && !force) {
                return false
            }

            isLoading = true

            loadingDisposable?.dispose()
            loadingDisposable = getPage(nextCursor)
                    .subscribeBy(
                            onSuccess = {
                                onNewItems(it.items)

                                isLoading = false
                                nextCursor = it.nextCursor
                                noMoreItems = it.isLast

                                updateResultSubject = null
                                resultSubject?.onComplete()
                            },
                            onError = {
                                isLoading = false
                                errorsSubject.onNext(it)

                                updateResultSubject = null
                                resultSubject?.onError(it)
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

        cacheNewItems(newItems)

        broadcast()

        if (newItems.isEmpty()) {
            noMoreItems = true
        }
    }

    override fun cacheNewItems(newItems: List<T>) {
        if (isOnFirstPage) {
            itemsCache.transform(newItems)
        } else {
            itemsCache.transform(newItems, { false })
        }
    }

    open fun loadMore(): Boolean {
        return loadMore(force = false, resultSubject = null)
    }

    private var updateResultSubject: CompletableSubject? = null

    override fun update(): Completable {
        return synchronized(this) {
            itemsCache.clear()
            nextCursor = null
            noMoreItems = false

            isLoading = false

            val resultSubject = updateResultSubject.let {
                if (it == null) {
                    val new = CompletableSubject.create()
                    updateResultSubject = new
                    new
                } else {
                    it
                }
            }

            loadMore(force = true, resultSubject = resultSubject)

            return@synchronized resultSubject
        }
    }

    // TODO: Implement me
    override fun getItems(): Single<List<T>> {
        return Single.error(NotImplementedError("Cannot get whole paged resource"))
    }
}