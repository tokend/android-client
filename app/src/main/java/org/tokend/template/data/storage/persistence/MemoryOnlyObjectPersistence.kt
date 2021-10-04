package org.tokend.template.data.storage.persistence

class MemoryOnlyObjectPersistence<T : Any> : ObjectPersistence<T> {
    private var item: T? = null

    override fun loadItem(): T? =
        item

    override fun saveItem(item: T) {
        this.item = item
    }

    override fun hasItem(): Boolean =
        item != null

    override fun clear() {
        item = null
    }
}