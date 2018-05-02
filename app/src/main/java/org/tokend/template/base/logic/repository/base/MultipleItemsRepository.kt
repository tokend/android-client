package org.tokend.template.base.logic.repository.base

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

/**
 * Repository that holds a list of [T] items.
 */
abstract class MultipleItemsRepository<T> : Repository() {
    protected abstract val itemsCache: RepositoryCache<T>

    val itemsSubject: BehaviorSubject<List<T>> =
            BehaviorSubject.createDefault<List<T>>(listOf())

    protected open fun broadcast() {
        itemsSubject.onNext(itemsCache.items)
    }

    protected abstract fun getItems(): Observable<List<T>>

    protected open fun onNewItems(newItems: List<T>) {
        isNeverUpdated = false
        isFresh = true

        itemsCache.merge(newItems)

        broadcast()
    }
}