package org.tokend.template.data.repository.base

import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.CompletableSubject

abstract class SimpleMultipleItemsRepository<T> : MultipleItemsRepository<T>() {
    private var updateResultSubject: CompletableSubject? = null

    private var updateDisposable: Disposable? = null
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
                        itemsCache.loadFromDb().doOnComplete { broadcast() }
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