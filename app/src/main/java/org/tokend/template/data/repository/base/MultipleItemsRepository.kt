package org.tokend.template.data.repository.base

import io.reactivex.Single
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

    protected abstract fun getItems(): Single<List<T>>

    protected open fun onNewItems(newItems: List<T>) {
        isNeverUpdated = false
        isFresh = true

        itemsCache.transform(newItems)

        broadcast()
    }
}