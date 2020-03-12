package org.tokend.template.data.repository.base.pagination

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.CompletableSubject
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.template.data.repository.base.Repository
import org.tokend.template.view.util.LoadingIndicatorManager

/**
 * Repository that loads and caches paged collection of [T].
 *
 * Caching works properly only if data is immutable!
 */
abstract class PagedDataRepository<T : PagingRecord>(
        protected open val pagingOrder: PagingOrder,
        open val cache: PagedDataCache<T>?
) : Repository() {
    protected open val pageLimit = DEFAULT_PAGE_LIMIT
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

    protected open val loadingStateManager = LoadingIndicatorManager(
            showLoading = { isLoading = true },
            hideLoading = { isLoading = false }
    )

    open var isLoadingTopPages: Boolean = false
        protected set

    private var updateResultSubject: CompletableSubject? = null
    private var loadMoreDuringUpdateDisposable: Disposable? = null
    override fun update(): Completable = synchronized(this) {
        isFresh = false
        mItems.clear()
        nextCursor = null
        noMoreItems = false

        val resultSubject = updateResultSubject.let {
            if (it == null) {
                val new = CompletableSubject.create()
                updateResultSubject = new
                new
            } else {
                it
            }
        }

        val loadMoreSubject = CompletableSubject.create()
        loadMore(force = true, resultSubject = loadMoreSubject)

        loadMoreDuringUpdateDisposable?.dispose()
        loadMoreDuringUpdateDisposable = loadMoreSubject
                .doOnTerminate { updateResultSubject = null }
                .subscribeBy(
                        onError = resultSubject::onError,
                        onComplete = {
                            resultSubject.onComplete()
                            if (!isFresh && pagingOrder == PagingOrder.DESC) {
                                loadNewRemoteTopPages()
                            }
                        }
                )

        return@synchronized resultSubject
    }

    protected open var loadNewRemoteTopPagesDisposable: Disposable? = null
    /**
     * Loads new pages to the top of collection if it's in DESC order
     */
    open fun loadNewRemoteTopPages() {
        Log.i(LOG_TAG, "Load new remote top pages")
        val newestItemId = mItems.firstOrNull()?.getPagingId() ?: 0L

        var nextNewPagesCursor: Long? = newestItemId
        var noMoreNewPages = false

        isLoadingTopPages = true
        loadingStateManager.show("new-top-pages")

        val processNextPage = Completable.defer {
            getAndCacheRemotePage(nextNewPagesCursor, PagingOrder.ASC)
                    .doOnSuccess { page ->
                        onNewRemoteTopPage(page)
                        nextNewPagesCursor = page.nextCursor?.toLong()
                        noMoreNewPages = page.isLast
                    }
                    .ignoreElement()
        }

        loadNewRemoteTopPagesDisposable?.dispose()
        loadNewRemoteTopPagesDisposable =
                processNextPage
                        .repeatUntil { noMoreNewPages }
                        .doOnEvent {
                            isLoadingTopPages = false
                            loadingStateManager.hide("new-top-pages")
                        }
                        .subscribeOn(Schedulers.newThread())
                        .subscribeBy(
                                onError = errorsSubject::onNext,
                                onComplete = {}
                        )
    }

    protected open fun onNewRemoteTopPage(page: DataPage<T>) {
        mItems.addAll(0, page.items.sortedByDescending(PagingRecord::getPagingId))
        isNeverUpdated = false
        if (page.isLast) {
            isFresh = true
        }
        if (page.items.isNotEmpty()) {
            broadcast()
        }
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
        if ((noMoreItems || (isLoading && !isLoadingTopPages)) && !force) {
            return false
        }

        loadingStateManager.show("load-more")

        val getPage: Single<DataPage<T>> =
                getCachedPage(nextCursor)
                        .flatMap { cachedPage ->
                            val wasOnFirstPage = isOnFirstPage
                            if (cachedPage.isLast) {
                                Log.i(LOG_TAG, "Cached page is last")
                                // If cached page is last emmit it
                                // but ensure that it is true by loading
                                // the same remote page.
                                if (cachedPage.items.isNotEmpty()) {
                                    onNewPage(cachedPage)
                                }
                                getAndCacheRemotePage(nextCursor, pagingOrder)
                                        .doOnSuccess {
                                            if (wasOnFirstPage) {
                                                isFresh = true
                                            }
                                        }
                            } else {
                                Log.i(LOG_TAG, "Accepted cached page")
                                Single.just(cachedPage)
                            }
                        }

        loadingDisposable?.dispose()
        loadingDisposable = getPage
                .doOnEvent { _, _ ->
                    loadingStateManager.hide("load-more")
                }
                .subscribeOn(Schedulers.newThread())
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

    protected open fun getAndCacheRemotePage(nextCursor: Long?,
                                             requiredOrder: PagingOrder): Single<DataPage<T>> {
        return getRemotePage(nextCursor, requiredOrder)
                .subscribeOn(Schedulers.newThread())
                .doOnSuccess(this::cachePage)
    }

    protected open fun cachePage(page: DataPage<T>) {
        cache?.cachePage(page)
    }

    abstract fun getRemotePage(nextCursor: Long?,
                               requiredOrder: PagingOrder): Single<DataPage<T>>

    open fun getCachedPage(nextCursor: Long?): Single<DataPage<T>> {
        return cache?.getPage(pageLimit, nextCursor, pagingOrder)
                ?: Single.just(DataPage(nextCursor?.toString(), emptyList(), true))
    }

    /**
     * Called when new page data is loaded, cached or remote.
     */
    protected open fun onNewPage(page: DataPage<T>) {
        mItems.addAll(page.items)
        nextCursor = page.nextCursor?.toLong()
        noMoreItems = page.isLast
        isNeverUpdated = false
        if (noMoreItems) {
            logDataHash()
        }
        broadcast()
    }

    protected open fun broadcast() {
        itemsSubject.onNext(mItems.toList())
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