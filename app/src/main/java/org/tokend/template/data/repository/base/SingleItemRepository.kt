package org.tokend.template.data.repository.base

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.CompletableSubject

/**
 * Repository that holds a single [T] item.
 */
abstract class SingleItemRepository<T: Any> : Repository() {
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

    protected abstract fun getItem(): Observable<T>

    protected open fun getStoredItem(): Observable<T> {
        return Observable.empty()
    }

    protected open fun storeItem(item: T) {}

    protected open fun onNewItem(newItem: T) {
        isNeverUpdated = false
        isFresh = true

        item = newItem

        broadcast()
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

            val storedItemObservable =
                    if (isNeverUpdated) getStoredItem() else Observable.empty()

            updateDisposable?.dispose()
            updateDisposable = storedItemObservable.concatWith(
                    getItem()
                            .map {
                                storeItem(it)
                                it
                            }
            )
                    .subscribeBy(
                            onNext = { newItem: T ->
                                onNewItem(newItem)
                            },
                            onComplete = {
                                isNeverUpdated = false
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