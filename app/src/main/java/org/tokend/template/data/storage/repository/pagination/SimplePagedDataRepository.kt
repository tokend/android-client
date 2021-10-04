package org.tokend.template.data.storage.repository.pagination

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.CompletableSubject
import org.tokend.template.data.storage.repository.Repository
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder

/**
 * Simple repository for paged data of type [T].
 * Use it if the data is mutable and you don't need caching (it doesn't have it).
 * Works with both cursor and page-number pagination
 */
abstract class SimplePagedDataRepository<T>(
        protected open val pagingOrder: PagingOrder = PagingOrder.DESC,
        protected open val pageLimit: Int = DEFAULT_PAGE_LIMIT,
) : Repository() {
    private var nextPage: String? = null

    open val itemsSubject = BehaviorSubject.createDefault(listOf<T>())
    protected open var mItems = mutableListOf<T>()

    open val itemsList: List<T>
        get() = itemsSubject.value ?: listOf()

    val isOnFirstPage: Boolean
        get() = nextPage == null

    var noMoreItems: Boolean = false
        protected set

    /**
     * @param page - cursor or number of the page to load
     */
    abstract fun getPage(
        limit: Int,
        page: String?,
        order: PagingOrder
    ): Single<DataPage<T>>

    protected var loadingDisposable: Disposable? = null
    protected open fun loadMore(
        force: Boolean,
        resultSubject: CompletableSubject?
    ): Boolean {
        synchronized(this) {
            if ((noMoreItems || isLoading) && !force) {
                return false
            }

            isLoading = true

            loadingDisposable?.dispose()
            loadingDisposable = getPage(pageLimit, nextPage, pagingOrder)
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onSuccess = {
                        onNewPage(it)

                        isLoading = false

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

    open fun onNewPage(page: DataPage<T>) {
        isNeverUpdated = false
        noMoreItems = page.isLast || page.items.isEmpty()

        if (pagingOrder == PagingOrder.DESC && isOnFirstPage
            || pagingOrder == PagingOrder.ASC && noMoreItems
        ) {
            isFresh = true
        }

        nextPage = page.nextCursor

        mItems.addAll(page.items)

        broadcast()
    }

    open fun loadMore(): Boolean {
        return loadMore(force = false, resultSubject = null)
    }

    private var updateResultSubject: CompletableSubject? = null

    override fun update(): Completable = synchronized(this) {
        mItems.clear()
        nextPage = null
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

        resultSubject
    }

    protected open fun broadcast() {
        itemsSubject.onNext(mItems.toList())
    }

    companion object {
        const val DEFAULT_PAGE_LIMIT = 20
    }
}