package org.tokend.template.logic.repository.base

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject

abstract class SimpleSingleItemRepository<T : Any> : SingleItemRepository<T>() {
    private var updateResultSubject: PublishSubject<Boolean>? = null

    private var updateDisposable: Disposable? = null
    override fun update(): Observable<Boolean> {
        return synchronized(this) {
            val resultSubject = updateResultSubject.let {
                if (it == null) {
                    val new = PublishSubject.create<Boolean>()
                    updateResultSubject = new
                    new
                } else {
                    it
                }
            }

            item = null
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
                                isLoading = false

                                updateResultSubject = null
                                resultSubject.onNext(true)
                                resultSubject.onComplete()
                            },
                            onError = {
                                isLoading = false
                                errorsSubject.onNext(it)

                                updateResultSubject = null
                                resultSubject.onError(it)
                                resultSubject.onComplete()
                            }
                    )

            resultSubject
        }
    }
}