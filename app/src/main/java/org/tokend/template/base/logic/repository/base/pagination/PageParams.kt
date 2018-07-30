package org.tokend.template.base.logic.repository.base.pagination

class PageParams(
        val limit: Int = DEFAULT_PAGE_SIZE,
        var cursor: String = DEFAULT_CURSOR
) {
    companion object {
        const val DEFAULT_CURSOR = "now"
        const val DEFAULT_PAGE_SIZE = 20
    }
}