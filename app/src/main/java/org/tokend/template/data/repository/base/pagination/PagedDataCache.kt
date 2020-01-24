package org.tokend.template.data.repository.base.pagination

import io.reactivex.Single
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder

interface PagedDataCache<T : PagingRecord> {
    fun getPage(limit: Int, cursor: Long?, order: PagingOrder): Single<DataPage<T>>
    fun cachePage(page: DataPage<T>)
    fun clear()
}