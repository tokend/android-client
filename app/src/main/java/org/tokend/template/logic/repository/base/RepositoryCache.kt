package org.tokend.template.logic.repository.base

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.template.util.ObservableTransformers

abstract class RepositoryCache<T> {
    protected open val mItems = mutableListOf<T>()
    open val items: List<T>
        get() = mItems.toList()

    protected open var isLoaded = false

    open fun loadFromDb(): Completable {
        synchronized(this ) {
            val initSingle =
                    if (!isLoaded)
                        getAllFromDb()
                                .doOnSuccess {
                                    isLoaded = true
                                    mItems.clear()
                                    mItems.addAll(it)
                                }
                    else
                        Single.just(listOf())

            return Completable.fromSingle(initSingle)
                    .compose(ObservableTransformers.defaultSchedulersCompletable())
        }
    }

    fun add(item: T): Boolean {
        return if (!mItems.contains(item)) {
            addToDb(listOf(item))
            mItems.add(0, item)
            true
        } else {
            false
        }
    }

    fun delete(item: T): Boolean {
        return mItems.remove(item).also { deleted ->
            if (deleted) {
                deleteFromDb(listOf(item))
            }
        }
    }

    fun update(item: T): Boolean {
        val index = mItems.indexOf(item)
        return if (index >= 0) {
            mItems[index] = item
            updateInDb(listOf(item))
            return true
        } else {
            false
        }
    }

    open fun merge(items: List<T>, filter: ((item: T) -> Boolean)? = null): Boolean {
        var changesOccurred = false

        val operatingItems =
                if (filter != null)
                    mItems.filter(filter)
                else
                    mItems

        if (operatingItems.isEmpty()) {
            if (items.isNotEmpty()) {
                mItems.addAll(items)
                addToDb(items)
                changesOccurred = true
            }
        } else if (items.isEmpty() && filter == null) {
            if (mItems.isNotEmpty()) {
                mItems.clear()
                clearDb()
                changesOccurred = true
            }
        } else {
            val newItems = mutableListOf<T>()
            val removedItems = mutableListOf<T>()
            val toUpdateInDb = mutableListOf<T>()

            items.forEach {
                if (!operatingItems.contains(it)) {
                    newItems.add(it)
                }
            }

            for (i in 0 until mItems.size) {
                val existing = mItems[i]

                if (filter != null && !operatingItems.contains(existing)) {
                    continue
                }

                val newIndex = items.indexOf(existing)
                if (newIndex < 0) {
                    removedItems.add(existing)
                } else {
                    val new = items[newIndex]
                    if (!isContentSame(new, existing)) {
                        mItems[i] = new
                        toUpdateInDb.add(new)
                        changesOccurred = true
                    }
                }
            }

            if (toUpdateInDb.isNotEmpty()) {
                updateInDb(toUpdateInDb)
            }

            if (removedItems.isNotEmpty()) {
                mItems.removeAll(removedItems)
                deleteFromDb(removedItems)
                changesOccurred = true
            }

            if (newItems.isNotEmpty()) {
                mItems.addAll(newItems)
                addToDb(newItems)
                changesOccurred = true
            }
        }

        if (changesOccurred) {
            sortItems()
        }

        return changesOccurred
    }

    protected open fun sortItems() { }

    // region Abstract

    protected abstract fun isContentSame(first: T, second: T): Boolean

    // region DB
    protected abstract fun getAllFromDb(): Single<List<T>>

    protected abstract fun addToDb(items: List<T>)

    protected abstract fun updateInDb(items: List<T>)

    protected abstract fun deleteFromDb(items: List<T>)

    protected abstract fun clearDb()
    // endregion
    // endregion
}