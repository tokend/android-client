package io.tokend.template.util

import java.util.*

/**
 * Contains different conditions of matching some query with given fields.
 */
object SearchUtil {
    private val WHITESPACE_REGEX = "\\s+".toRegex()

    /**
     * Query is being lowercased and splitted by whitespace.
     * Match is confirmed if all query parts are matched with given fields
     * by [String.startsWith] condition.
     *
     * For example, for person's fields "John" (first name) and "Doe" (last name) the following
     * queries will be matched: "john", "jo", "j", "doe", "d", "j d", "jo do", "john doe", "doe john", etc.
     * The same match condition is implemented in Android contacts app.
     *
     * @param query search query
     * @param fields entity fields to match query
     */
    fun isMatchGeneralCondition(query: String, vararg fields: String?): Boolean {
        val unmatchedFieldsParts = fields.fold(mutableSetOf<String>()) { acc, item ->
            if (item != null) {
                acc.addAll(splitByWhitespace(item.toLowerCase(Locale.getDefault())))
            }
            acc
        }

        val unmatchedQueryParts = splitByWhitespace(query.toLowerCase(Locale.getDefault())).toMutableList()
        var unmatchedChanged = true
        while (unmatchedFieldsParts.isNotEmpty()
            && unmatchedQueryParts.isNotEmpty()
            && unmatchedChanged
        ) {
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
        return text.split(WHITESPACE_REGEX)
    }
}