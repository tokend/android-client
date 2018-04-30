package org.tokend.template.logic.repository.base

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject

abstract class SimpleMultipleItemsRepository<T> : MultipleItemsRepository<T>() {
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

            isLoading = true

            val loadItemsFromDb =
                    if (isNeverUpdated)
                        itemsCache.loadFromDb().doOnComplete { broadcast() }
                    else
                        Completable.complete()

            updateDisposable?.dispose()
            updateDisposable = loadItemsFromDb.andThen(getItems())
                    .subscribeBy(
                            onNext = { items ->
                                onNewItems(items)
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