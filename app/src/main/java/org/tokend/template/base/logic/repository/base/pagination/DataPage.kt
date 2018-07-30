package org.tokend.template.base.logic.repository.base.pagination

import org.tokend.sdk.api.responses.Page
import java.util.regex.Pattern

/**
 * Light-weight version of the [Page].
 */
class DataPage<T>(val nextCursor: String?,
                  val items: List<T>,
                  val isLast: Boolean = false) {
    companion object {
        fun <T> getNextCursor(page: Page<T>): String? {
            val nextLink = page.getLinks()?.next?.href ?: return null
            return getNumberParamFromLink(nextLink, "cursor")
        }

        private fun getNumberParamFromLink(link: String, param: String): String? {
            val pattern = Pattern.compile("$param=(\\d+)")
            val matcher = pattern.matcher(link)
            return if (matcher.find()) {
                matcher.group(matcher.groupCount())
            } else {
                null
            }
        }

        fun <T> isLast(page: Page<T>): Boolean {
            val selfLink = page.getLinks()?.self?.href ?: return true
            val limit =
                    getNumberParamFromLink(selfLink, "limit")?.toIntOrNull() ?: return true
            return page.records.size < limit
        }

        fun <T> fromPage(page: Page<T>): DataPage<T> {
            return DataPage(getNextCursor(page), page.records, isLast(page))
        }
    }
}