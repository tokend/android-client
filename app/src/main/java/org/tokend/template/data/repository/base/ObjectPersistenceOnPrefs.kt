package org.tokend.template.data.repository.base

import android.content.SharedPreferences
import com.google.gson.Gson
import org.tokend.sdk.factory.GsonFactory

/**
 * Implements persistence for an object of type [T]
 * based on [SharedPreferences] with [Gson] serialization
 */
open class ObjectPersistenceOnPrefs<T : Any>(
        protected open val itemClass: Class<T>,
        protected open val preferences: SharedPreferences,
        protected open val key: String
) : ObjectPersistence<T> {
    protected open var loadedItem: T? = null
    protected open val gson = GsonFactory().getBaseGson()

    override fun loadItem(): T? {
        return loadedItem
                ?: preferences
                        .getString(key, "")
                        .takeIf(String::isNotEmpty)
                        ?.let(this::deserializeItem)
                        ?.also { loadedItem = it }
    }

    override fun saveItem(item: T) {
        loadedItem = item
        preferences
                .edit()
                .putString(key, serializeItem(item))
                .apply()
    }

    override fun hasItem(): Boolean {
        return loadItem() != null
    }

    override fun clear() {
        loadedItem = null
        preferences
                .edit()
                .remove(key)
                .apply()
    }

    protected open fun serializeItem(item: T): String =
            gson.toJson(item)

    protected open fun deserializeItem(serialized: String): T? =
            try {
                gson.fromJson(serialized, itemClass)
            } catch (_: Exception) {
                null
            }
}