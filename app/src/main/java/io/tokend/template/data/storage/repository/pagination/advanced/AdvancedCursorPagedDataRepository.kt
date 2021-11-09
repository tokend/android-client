package io.tokend.template.data.storage.repository.pagination.advanced

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.CompletableSubject
import io.tokend.template.data.storage.repository.Repository
import io.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder

/**
 * Repository that intelligently loads and caches paged collection of [CursorPagingRecord].
 * It does full data caching while you "scroll" through it, so it allows offline browsing next time.
 * On update it loads new data from the top in case if [PagingOrder.DESC] order is used.
 *
 * Works ONLY with numeric cursor-based pagination and immutable data (such as histories)!
 */
abstract class AdvancedCursorPagedDataRepository<T : CursorPagingRecord>(
    protected open val pagingOrder: PagingOrder = PagingOrder.DESC,
    protected open val pageLimit: Int = DEFAULT_PAGE_LIMIT,
    open val cache: CursorPagedDataCache<T>?,
) : Repository() {
    protected var nextCursor: Long? = null

    val isOnFirstPage: Boolean
        get() = nextCursor == null
                || pagingOrder == PagingOrder.ASC && nextCursor == 0L

    var noMoreItems: Boolean = false
        protected set

    open val itemsSubject = BehaviorSubject.createDefault(listOf<T>())
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
        val newestItemCursor = mItems.firstOrNull()?.pagingCursor ?: 0L
        Log.i(LOG_TAG, "Load new remote top pages from $newestItemCursor")

        var nextNewPagesCursor: Long? = newestItemCursor
        var noMoreNewPages = false

        isLoadingTopPages = true
        loadingStateManager.show("new-top-pages")

        val processNextPage = Completable.defer {
            getAndCacheRemotePage(pageLimit, nextNewPagesCursor, PagingOrder.ASC)
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
        mItems.addAll(0, page.items.sortedByDescending(CursorPagingRecord::pagingCursor))
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
    protected open fun loadMore(
        force: Boolean,
        resultSubject: CompletableSubject?
    ): Boolean = synchronized(this) {
        if ((noMoreItems || (isLoading && !isLoadingTopPages)) && !force) {
            return false
        }

        loadingStateManager.show("load-more")

        val getPage: Single<DataPage<T>> =
            getCachedPage(nextCursor)
                .flatMap { cachedPage ->
                    if (cachedPage.isLast) {
                        Log.i(LOG_TAG, "Cached page is last")
                        // If cached page is last emmit it
                        // but ensure that it is true by loading
                        // the same remote page.
                        if (cachedPage.items.isNotEmpty()) {
                            onNewPage(cachedPage)
                        }
                        val wasOnFirstPage = isOnFirstPage
                        getAndCacheRemotePage(pageLimit, nextCursor, pagingOrder)
                            .doOnSuccess { page ->
                                if ((pagingOrder == PagingOrder.ASC || wasOnFirstPage)
                                    && page.isLast
                                ) {
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

    protected open fun getAndCacheRemotePage(
        limit: Int,
        cursor: Long?,
        requiredOrder: PagingOrder
    ): Single<DataPage<T>> {
        return getRemotePage(limit, cursor, requiredOrder)
            .subscribeOn(Schedulers.newThread())
            .doOnSuccess(this::cachePage)
    }

    protected open fun cachePage(page: DataPage<T>) {
        cache?.cachePage(page)
    }

    /**
     * @param cursor MUST be used as a page[[cursor]]
     */
    abstract fun getRemotePage(
        limit: Int,
        cursor: Long?,
        requiredOrder: PagingOrder
    ): Single<DataPage<T>>

    open fun getCachedPage(cursor: Long?): Single<DataPage<T>> {
        return cache?.getPage(pageLimit, cursor, pagingOrder)
            ?: Single.just(DataPage(cursor?.toString(), emptyList(), true))
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
            .map(CursorPagingRecord::pagingCursor)
            .toTypedArray()
            .contentHashCode()
        Log.i(LOG_TAG, "Final data hash is $hash")
    }

    companion object {
        private const val LOG_TAG = "AdvPagedRepo"
        const val DEFAULT_PAGE_LIMIT = 20
    }
}