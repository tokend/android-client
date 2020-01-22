package org.tokend.template.data.repository.base

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.CompletableSubject

/**
 * Repository that holds a list of [T] items.
 */
abstract class MultipleItemsRepository<T>(val itemsCache: RepositoryCache<T>) : Repository() {
    private var ensureDataResultSubject: CompletableSubject? = null
    private var ensureDataDisposable: Disposable? = null

    private var updateResultSubject: CompletableSubject? = null
    private var updateDisposable: Disposable? = null

    /**
     * BehaviourSubject which emits repository items on changes,
     * initialized with empty list
     */
    val itemsSubject: BehaviorSubject<List<T>> =
            BehaviorSubject.createDefault<List<T>>(listOf())

    /**
     * Repository items
     */
    val itemsList: List<T>
        get() = itemsSubject.value ?: itemsCache.items

    protected open fun broadcast() {
        itemsSubject.onNext(itemsCache.items)
    }

    protected abstract fun getItems(): Single<List<T>>

    protected open fun onNewItems(newItems: List<T>) {
        isNeverUpdated = false
        isFresh = true

        cacheNewItems(newItems)

        broadcast()
    }

    protected open fun cacheNewItems(newItems: List<T>) {
        itemsCache.transform(newItems)
    }

    /**
     * Ensures that repository contains a data if it was never updated:
     * loads data from cache, if the cache is empty then performs [updateDeferred]
     */
    open fun ensureData(): Completable = synchronized(this) {
        val resultSubject = ensureDataResultSubject.let {
            if (it == null) {
                val new = CompletableSubject.create()
                ensureDataResultSubject = new
                new
            } else {
                it
            }
        }

        if (!isNeverUpdated) {
            ensureDataResultSubject = null
            resultSubject.onComplete()
        } else {
            isLoading = true

            ensureDataDisposable?.dispose()
            ensureDataDisposable = itemsCache
                    .loadFromDb()
                    .onErrorComplete()
                    .andThen(Completable.defer {
                        if (itemsCache.items.isNotEmpty()) {
                            broadcast()
                            Completable.complete()
                        } else {
                            updateDeferred()
                        }
                    })
                    .subscribeBy(
                            onComplete = {
                                isNeverUpdated = false
                                isLoading = false
                                ensureDataResultSubject = null
                                resultSubject.onComplete()
                            },
                            onError = {
                                isLoading = false
                                ensureDataResultSubject = null
                                errorsSubject.onNext(it)
                                resultSubject.onError(it)
                            }
                    )
        }

        resultSubject
    }

    override fun update(): Completable {
        invalidate()

        return synchronized(this) {
            val resultSubject = updateResultSubject.let {
                if (it == null) {
                    val new = CompletableSubject.create()
                    updateResultSubject = new
                    new
                } else {
                    it
                }
            }

            isLoading = true

            val loadItemsFromDb =
                    if (isNeverUpdated)
                        itemsCache.loadFromDb().doOnComplete {
                            isNeverUpdated = false
                            broadcast()
                        }
                    else
                        Completable.complete()

            updateDisposable?.dispose()
            updateDisposable = loadItemsFromDb.andThen(getItems())
                    .subscribeBy(
                            onSuccess = { items ->
                                onNewItems(items)

                                isLoading = false
                                updateResultSubject = null
                                resultSubject.onComplete()
                            },
                            onError = {
                                isLoading = false
                                errorsSubject.onNext(it)

                                updateResultSubject = null
                                resultSubject.onError(it)
                            }
                    )

            resultSubject
        }
    }
}