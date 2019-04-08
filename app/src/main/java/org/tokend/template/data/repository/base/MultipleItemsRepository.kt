package org.tokend.template.data.repository.base

import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject

/**
 * Repository that holds a list of [T] items.
 */
abstract class MultipleItemsRepository<T>( val itemsCache: RepositoryCache<T>) : Repository() {

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
}