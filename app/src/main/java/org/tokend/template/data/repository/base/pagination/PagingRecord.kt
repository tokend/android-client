package org.tokend.template.data.repository.base.pagination

/**
 * Interface for records, collections of
 * which are paged.
 */
interface PagingRecord {
    fun  getPagingId(): Long
}