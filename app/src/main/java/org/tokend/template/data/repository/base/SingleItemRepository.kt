package org.tokend.template.data.repository.base

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

/**
 * Repository that holds a single [T] item.
 */
abstract class SingleItemRepository<T> : Repository() {
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

    abstract protected fun getItem(): Observable<T>

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
}