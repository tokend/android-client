package org.tokend.template.data.repository.base

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.doAsync

/**
 * Represents in-memory cache of given type with persistence.
 */
abstract class RepositoryCache<T> {
    protected open val mItems = mutableListOf<T>()
    open val items: List<T>
        get() = mItems.toList()

    protected open var isLoaded = false

    /**
     * Will replace current items with loaded from database.
     */
    open fun loadFromDb(): Completable {
        synchronized(this) {
            val initSingle =
                    if (!isLoaded)
                        getAllFromDbSafe()
                                .doOnSuccess {
                                    isLoaded = true
                                    mItems.clear()
                                    mItems.addAll(it)
                                }
                    else
                        Single.just(listOf())

            return Completable.fromSingle(initSingle).observeOn(Schedulers.io())
        }
    }

    fun add(item: T): Boolean {
        return if (!mItems.contains(item)) {
            addToDbSafe(listOf(item))
            mItems.add(0, item)
            true
        } else {
            false
        }
    }

    fun delete(item: T): Boolean {
        return mItems.remove(item).also { deleted ->
            if (deleted) {
                deleteFromDbSafe(listOf(item))
            }
        }
    }

    fun update(item: T): Boolean {
        val index = mItems.indexOf(item)
        return if (index >= 0) {
            mItems[index] = item
            updateInDbSafe(listOf(item))
            return true
        } else {
            false
        }
    }

    fun updateOrAdd(item: T): Boolean {
        return if (update(item))
            true
        else
            add(item)
    }

    /**
     * Intelligently transforms current item set to the given one.
     * You can pass an empty list to clear the cache
     * or pass an items list and a false filter to just add them into cache.
     * @param newStateItems new list of items
     * @param filter predicate to form custom initial state from current items.
     */
    open fun transform(newStateItems: List<T>, filter: ((item: T) -> Boolean)? = null): Boolean {
        var changesOccurred = false

        val operatingItems =
                if (filter != null)
                    mItems.filter(filter)
                else
                    mItems

        if (operatingItems.isEmpty()) {
            if (newStateItems.isNotEmpty()) {
                mItems.addAll(newStateItems)
                addToDbSafe(newStateItems)
                changesOccurred = true
            }
        } else if (newStateItems.isEmpty() && filter == null) {
            if (mItems.isNotEmpty()) {
                mItems.clear()
                clearDbSafe()
                changesOccurred = true
            }
        } else {
            val newItems = mutableListOf<T>()
            val removedItems = mutableListOf<T>()
            val toUpdateInDb = mutableListOf<T>()

            newStateItems.forEach {
                if (!operatingItems.contains(it)) {
                    newItems.add(it)
                }
            }

            for (i in 0 until mItems.size) {
                val existing = mItems[i]

                if (filter != null && !operatingItems.contains(existing)) {
                    continue
                }

                val newIndex = newStateItems.indexOf(existing)
                if (newIndex < 0) {
                    removedItems.add(existing)
                } else {
                    val new = newStateItems[newIndex]
                    if (!isContentSame(new, existing)) {
                        mItems[i] = new
                        toUpdateInDb.add(new)
                        changesOccurred = true
                    }
                }
            }

            if (toUpdateInDb.isNotEmpty()) {
                updateInDbSafe(toUpdateInDb)
            }

            if (removedItems.isNotEmpty()) {
                mItems.removeAll(removedItems)
                deleteFromDbSafe(removedItems)
                changesOccurred = true
            }

            if (newItems.isNotEmpty()) {
                mItems.addAll(newItems)
                addToDbSafe(newItems)
                changesOccurred = true
            }
        }

        if (changesOccurred) {
            sortItems()
        }

        return changesOccurred
    }

    open fun clear() {
        transform(emptyList())
    }

    /**
     * Sorts inner items set if implemented.
     */
    protected open fun sortItems() {}

    private fun getAllFromDbSafe(): Single<List<T>> {
        return Single.defer {
            val items = synchronized(this@RepositoryCache) {
                getAllFromDb()
            }

            Single.just(items)
        }.subscribeOn(Schedulers.io())
    }

    private fun addToDbSafe(items: List<T>) {
        doAsync {
            synchronized(this@RepositoryCache) {
                addToDb(items)
            }
        }
    }

    private fun updateInDbSafe(items: List<T>) {
        doAsync {
            synchronized(this@RepositoryCache) {
                updateInDb(items)
            }
        }
    }

    private fun deleteFromDbSafe(items: List<T>) {
        doAsync {
            synchronized(this@RepositoryCache) {
                deleteFromDb(items)
            }
        }
    }

    private fun clearDbSafe() {
        doAsync {
            synchronized(this@RepositoryCache) {
                clearDb()
            }
        }
    }

    // region Abstract

    /**
     * @return true if items are technically not equals but have the same content.
     */
    protected abstract fun isContentSame(first: T, second: T): Boolean

    // region DB
    protected abstract fun getAllFromDb(): List<T>

    protected abstract fun addToDb(items: List<T>)

    protected abstract fun updateInDb(items: List<T>)

    protected abstract fun deleteFromDb(items: List<T>)

    protected abstract fun clearDb()
    // endregion
    // endregion
}