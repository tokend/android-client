package org.tokend.template.extensions

import androidx.collection.LruCache

inline fun <K, V> androidx.collection.LruCache<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue()
        put(key, answer)
        answer
    } else {
        value
    }
}