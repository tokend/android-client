package org.tokend.template.data.repository.base

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.CompletableSubject

/**
 * Repository that holds a single [T] item.
 */
abstract class SingleItemRepository<T : Any>(
        protected open val itemPersistence: ObjectPersistence<T>? = null
) : Repository() {
    private var ensureDataResultSubject: CompletableSubject? = null
    private var ensureDataDisposable: Disposable? = null

    private var updateResultSubject: CompletableSubject? = null
    private var updateDisposable: Disposable? = null

    /**
     * Repository item
     */
    var item: T? = null
        protected set

    /**
     * BehaviourSubject which emits repository item on changes
     */
    val itemSubject: BehaviorSubject<T> = BehaviorSubject.create()

    protected open fun broadcast() {
        item?.let { itemSubject.onNext(it) }
    }

    protected abstract fun getItem(): Maybe<T>

    protected open fun getStoredItem(): Maybe<T> {
        return itemPersistence
                ?.let { Maybe.defer { it.loadItem().toMaybe() } }
                ?: Maybe.empty()
    }

    protected open fun storeItem(item: T) =
            itemPersistence?.saveItem(item)

    protected open fun onNewItem(newItem: T) {
        isNeverUpdated = false
        isFresh = true

        item = newItem

        broadcast()
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
            ensureDataDisposable = getStoredItem()
                    .doOnSuccess {
                        item = it
                    }
                    .toSingle()
                    .ignoreElement()
                    .onErrorComplete()
                    .andThen(Completable.defer {
                        if (item != null) {
                            broadcast()
                            Completable.complete()
                        } else {
                            updateDeferred()
                        }
                    })
                    .subscribeOn(Schedulers.newThread())
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

            val storedItemMaybe =
                    if (isNeverUpdated)
                        getStoredItem()
                    else
                        Maybe.empty()

            updateDisposable?.dispose()
            updateDisposable = storedItemMaybe
                    .toObservable()
                    .concatWith(
                            getItem()
                                    .doOnSuccess {
                                        storeItem(it)
                                    }
                    )
                    .subscribeOn(Schedulers.newThread())
                    .subscribeBy(
                            onNext = { newItem: T ->
                                isNeverUpdated = false
                                onNewItem(newItem)
                            },
                            onComplete = {
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