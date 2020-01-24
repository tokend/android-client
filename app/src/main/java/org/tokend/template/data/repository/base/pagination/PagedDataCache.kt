package org.tokend.template.data.repository.base.pagination

import io.reactivex.Single
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingParamsHolder

interface PagedDataCache<T: Any> {
    fun getPage(pagingParams: PagingParamsHolder): Single<DataPage<T>>
    fun cachePage(page: DataPage<T>)
    fun clear()
}