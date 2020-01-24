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
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.template.data.repository.base.Repository

abstract class SuperDescPagedDataRepository<T : Any>(
        open val cache: PagedDataCache<T>
) : Repository() {
    protected val pageLimit = DEFAULT_PAGE_LIMIT
    protected var nextCursor: String? = null

    val isOnFirstPage: Boolean
        get() = nextCursor == null

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

    open fun loadMore(): Boolean =
            loadMore(force = false, resultSubject = null)

    private var loadingDisposable: Disposable? = null
    protected open fun loadMore(force: Boolean,
                                resultSubject: CompletableSubject?): Boolean = synchronized(this) {
        if ((noMoreItems || isLoading) && !force) {
            return false
        }

        isLoading = true

        val nextCursor = this.nextCursor

        val getPage: Single<DataPage<T>> =
                if (isOnFirstPage)
                    getAndCacheRemotePage(nextCursor)
                            .onErrorResumeNext {
                                Log.i("Oleg", "Remote page loading error $it")
                                getCachedPage(nextCursor)
                            }
                else
                    getCachedPage(nextCursor)
                            .flatMap { cachedPage ->
                                if (cachedPage.isLast) {
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

    protected open fun getAndCacheRemotePage(nextCursor: String?): Single<DataPage<T>> {
        Log.i("Oleg", "Get remote page $nextCursor")
        return getRemotePage(nextCursor)
                .doOnSuccess(this::cachePage)
    }

    protected open fun cachePage(page: DataPage<T>) =
            cache.cachePage(page)

    abstract fun getRemotePage(nextCursor: String?): Single<DataPage<T>>

    open fun getCachedPage(nextCursor: String?): Single<DataPage<T>> {
        Log.i("Oleg", "Get cached page $nextCursor")
        return cache.getPage(PagingParamsV2(
                order = PagingOrder.DESC,
                limit = pageLimit,
                page = nextCursor
        ))
    }

    protected open fun onNewPage(page: DataPage<T>) {
        mItems.addAll(page.items)
        nextCursor = page.nextCursor
        noMoreItems = page.isLast
        broadcast()
    }

    protected open fun broadcast() {
        itemsSubject.onNext(mItems)
    }

    companion object {
        const val DEFAULT_PAGE_LIMIT = 20
    }
}