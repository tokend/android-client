package org.tokend.template.features.invest.repository

import io.reactivex.Single
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.repository.AccountDetailsRepository
import org.tokend.template.base.logic.repository.base.pagination.DataPage
import org.tokend.template.base.logic.repository.base.pagination.PageParams
import org.tokend.template.base.logic.repository.base.pagination.PagedDataRepository
import org.tokend.template.base.logic.repository.base.pagination.PagedRequestParams
import org.tokend.template.extensions.Sale
import org.tokend.template.extensions.toSingle

class SalesRepository(
        private val apiProvider: ApiProvider,
        private val accountDetailsRepository: AccountDetailsRepository? = null
) : PagedDataRepository<Sale, SalesRepository.SalesRequestParams>() {
    class SalesRequestParams(val name: String? = null,
                             val baseAsset: String? = null,
                             val openOnly: Boolean = true,
                             pageParams: PageParams = PageParams()) : PagedRequestParams(pageParams)

    override fun getItems(): Single<List<Sale>> = Single.just(emptyList())

    override val itemsCache = SalesCache()

    override fun getPage(requestParams: SalesRequestParams): Single<DataPage<Sale>> {
        var sales = listOf<Sale>()
        var nextCursor: String? = ""
        var isLast = false
        return apiProvider.getApi()
                .getSales(
                        limit = requestParams.pageParams.limit,
                        cursor = requestParams.pageParams.cursor,
                        order = "desc",
                        name = requestParams.name,
                        baseAsset = requestParams.baseAsset,
                        openOnly = requestParams.openOnly
                )
                .toSingle()
                .map {
                    nextCursor = DataPage.getNextCursor(it)
                    isLast = DataPage.isLast(it)
                    it.records
                }
                .map { records ->
                    sales = records
                    sales.map { it.ownerAccount }
                }
                .flatMap { ownerAccounts ->
                    accountDetailsRepository
                            ?.getDetails(ownerAccounts)
                            ?.onErrorReturnItem(emptyMap())
                            ?: Single.just(emptyMap())
                }
                .map { owners ->
                    sales.forEach {
                        it.ownerDetails = owners[it.ownerAccount]
                    }

                    sales
                }
                .map { DataPage(nextCursor, it, isLast) }
    }

    fun getSingle(id: Long): Single<Sale> {
        return apiProvider.getApi()
                .getSale(id)
                .toSingle()
                .flatMap { sale ->
                    val saleOwner = sale.ownerAccount
                    accountDetailsRepository?.getDetails(listOf(saleOwner))
                            ?.map {
                                sale.ownerDetails = it[saleOwner]
                                sale
                            } ?: Single.just(sale)
                }
    }

    override fun getNextPageRequestParams(): SalesRequestParams {
        return SalesRequestParams(pageParams = PageParams(cursor = nextCursor))
    }
}