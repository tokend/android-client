package org.tokend.template.data.repository.base.pagination

import io.reactivex.Single
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder

/**
 * Cache of [PagingRecord] ordered by [PagingRecord.getPagingId].
 * You can't add new records to it unless their ID's are known,
 * otherwise it will break pagination.
 */
interface PagedDataCache<T : PagingRecord> {
    fun getPage(limit: Int, cursor: Long?, order: PagingOrder): Single<DataPage<T>>
    fun cachePage(page: DataPage<T>)
    fun update(vararg items: T)
    fun delete(vararg items: T)
    fun clear()
}