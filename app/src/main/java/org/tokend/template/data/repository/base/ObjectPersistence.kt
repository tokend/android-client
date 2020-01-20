package org.tokend.template.data.repository.base

interface ObjectPersistence<T: Any> {
    fun loadItem(): T?
    fun saveItem(item: T)
    fun hasItem(): Boolean
    fun clear()
}