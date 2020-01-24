package org.tokend.template.data.repository.base.pagination

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.CompletableSubject
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.template.data.repository.base.Repository

/**
 * Repository that loads and caches paged collection of [T].
 *
 * Caching works properly only if data is immutable!
 */
abstract class SuperPagedDataRepository<T : PagingRecord>(
        protected open val pagingOrder: PagingOrder,
        open val cache: PagedDataCache<T>?
) : Repository() {
    protected val pageLimit = DEFAULT_PAGE_LIMIT
    protected var nextCursor: Long? = null

    val isOnFirstPage: Boolean
        get() = nextCursor == null
                || pagingOrder == PagingOrder.ASC && nextCursor == 0L

    var noMoreItems: Boolean = false
        protected set

    open val itemsSubject = BehaviorSubject.create<List<T>>()
    protected open var mItems = mutableListOf<T>()

    open val itemsList: List<T>
        get() = itemsSubject.value ?: listOf()

    private var updateResultSubject: CompletableSubject? = null
    override fun update(): Completable = synchronized(this) {
        mItems.clear()
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

    /**
     * Requests next page loading if it's necessary
     *
     * @return true if loading will be performed, false otherwise
     */
    open fun loadMore(): Boolean =
            loadMore(force = false, resultSubject = null)

    private var loadingDisposable: Disposable? = null
    protected open fun loadMore(force: Boolean,
                                resultSubject: CompletableSubject?): Boolean = synchronized(this) {
        if ((noMoreItems || isLoading) && !force) {
            return false
        }

        isLoading = true

        val getPage: Single<DataPage<T>> =
                if (pagingOrder == PagingOrder.DESC && isOnFirstPage)
                // First page in DESC order is a source of new items,
                // it must be actual so load the remote one.
                    getAndCacheRemotePage(nextCursor)
                            // But if remote loading failed we can display cached one.
                            .onErrorResumeNext { error ->
                                getCachedPage(nextCursor)
                                        .doOnSuccess { cachedPage ->
                                            if (cachedPage.items.isNotEmpty()) {
                                                onNewPage(cachedPage)
                                            }
                                        }
                                        .flatMap { Single.error<DataPage<T>>(error) }
                            }
                else
                // Otherwise we prefer lo load cached data as it is immutable.
                    getCachedPage(nextCursor)
                            .flatMap { cachedPage ->
                                if (cachedPage.isLast) {
                                    // If cached page is last emmit it
                                    // but ensure that it is true by loading
                                    // the same remote page.
                                    if (cachedPage.items.isNotEmpty()) {
                                        onNewPage(cachedPage)
                                    }
                                    getAndCacheRemotePage(nextCursor)
                                } else {
                                    Single.just(cachedPage)
                                }
                            }

        loadingDisposable?.dispose()
        loadingDisposable = getPage
                .doOnEvent { _, _ ->
                    isLoading = false
                }
                .subscribeBy(
                        onSuccess = {
                            onNewPage(it)
                            resultSubject?.onComplete()
                        },
                        onError = {
                            errorsSubject.onNext(it)
                            resultSubject?.onError(it)
                        }
                )


        return true
    }

    protected open fun getAndCacheRemotePage(nextCursor: Long?): Single<DataPage<T>> {
        return getRemotePage(nextCursor)
                .doOnSuccess(this::cachePage)
    }

    protected open fun cachePage(page: DataPage<T>) {
        cache?.cachePage(page)
    }

    abstract fun getRemotePage(nextCursor: Long?): Single<DataPage<T>>

    open fun getCachedPage(nextCursor: Long?): Single<DataPage<T>> {
        return cache?.getPage(pageLimit, nextCursor, pagingOrder)
                ?: Single.just(DataPage(nextCursor?.toString(), emptyList(), true))
    }

    /**
     * Called when new page data is loaded, cached or remote.
     */
    protected open fun onNewPage(page: DataPage<T>) {
        mItems.addAll(page.items)
        isNeverUpdated = false
        if (isOnFirstPage) {
            isFresh = true
        }
        nextCursor = page.nextCursor?.toLong()
        noMoreItems = page.isLast
        if (noMoreItems) {
            logDataHash()
        }
        broadcast()
    }

    protected open fun broadcast() {
        itemsSubject.onNext(mItems)
    }

    private fun logDataHash() {
        val hash = mItems
                .map(PagingRecord::getPagingId)
                .toTypedArray()
                .contentHashCode()
        Log.i(LOG_TAG, "Final data hash is $hash")
    }

    companion object {
        private const val LOG_TAG = "PagedRepo"
        const val DEFAULT_PAGE_LIMIT = 20
    }
}