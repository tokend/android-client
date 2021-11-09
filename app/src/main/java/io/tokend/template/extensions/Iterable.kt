package io.tokend.template.extensions

/**
 * Returns a list containing only the successful (i.e. without exception)
 * results of applying the given [transform] function
 * to each element in the original collection.
 */
inline fun <T, R : Any> Iterable<T>.mapSuccessful(transform: (T) -> R): List<R> {
    return this.mapNotNull { tryOrNull { transform(it) } }
}