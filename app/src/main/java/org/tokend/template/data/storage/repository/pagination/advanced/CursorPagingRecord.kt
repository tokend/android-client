package org.tokend.template.data.storage.repository.pagination.advanced

/**
 * Interface for records, collections of
 * which are paged by cursor.
 */
interface CursorPagingRecord {
    /**
     * Chronological unique identifier of the record (ID or timestamp)
     */
    val pagingCursor: Long
}