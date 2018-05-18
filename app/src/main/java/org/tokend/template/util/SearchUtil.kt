package org.tokend.template.util

import com.google.common.base.CharMatcher

/**
 * Contains different conditions of matching some query with given fields.
 */
object SearchUtil {
    private const val WHITESPACE = " "
    private val whitespaceMatcher = CharMatcher.WHITESPACE.or(CharMatcher.BREAKING_WHITESPACE)

    fun isMatchGeneralCondition(query: String, vararg fields: String?): Boolean {
        val unmatchedFieldsParts = fields.fold(mutableSetOf<String>()) { acc, item ->
            if (item != null) {
                acc.addAll(splitByWhitespace(item))
            }
            acc
        }

        val unmatchedQueryParts = splitByWhitespace(query).toMutableList()
        var unmatchedChanged = true
        while (unmatchedFieldsParts.isNotEmpty()
                && unmatchedQueryParts.isNotEmpty()
                && unmatchedChanged) {
            val unmatchedFieldsPartsIterator = unmatchedFieldsParts.iterator()
            unmatchedChanged = false
            while (unmatchedFieldsPartsIterator.hasNext()) {
                val fieldPart = unmatchedFieldsPartsIterator.next()

                val partsIterator = unmatchedQueryParts.iterator()
                while (partsIterator.hasNext()) {
                    val queryPart = partsIterator.next()

                    if (fieldPart.startsWith(queryPart, true)) {
                        partsIterator.remove()
                        unmatchedFieldsPartsIterator.remove()
                        unmatchedChanged = true
                        break
                    }
                }
            }
        }

        return unmatchedQueryParts.isEmpty()
    }

    private fun splitByWhitespace(text: String): Collection<String> {
        return whitespaceMatcher
                .replaceFrom(text, WHITESPACE)
                .split(WHITESPACE)
    }
}