package io.tokend.template.data.storage.repository

/**
 * Repository cache without persistence
 */
open class MemoryOnlyRepositoryCache<T> : RepositoryCache<T>() {
    override fun isContentSame(first: T, second: T): Boolean = false

    override fun getAllFromDb(): List<T> = emptyList()

    override fun addToDb(items: Collection<T>) {}

    override fun updateInDb(items: Collection<T>) {}

    override fun deleteFromDb(items: Collection<T>) {}

    override fun clearDb() {}
}