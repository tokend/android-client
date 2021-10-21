package org.tokend.template.extensions

inline fun <T> List<T>.forEachReversedByIndex(receiver: (T) -> Unit) {
    for (index in this.indices.reversed()) {
        receiver.invoke(this[index])
    }
}