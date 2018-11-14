package org.tokend.template.features.invest.repository

import io.reactivex.Single
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.sales.params.SalesParams
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.repository.AccountDetailsRepository
import org.tokend.template.base.logic.repository.base.pagination.PagedDataRepository
import org.tokend.template.extensions.Sale
import org.tokend.template.extensions.toSingle

class SalesRepository(
        private val apiProvider: ApiProvider,
        private val accountDetailsRepository: AccountDetailsRepository? = null
) : PagedDataRepository<Sale, SalesParams>() {

    private var name: String? = null
    private var baseAsset: String? = null

    override fun getItems(): Single<List<Sale>> = Single.just(emptyList())

    override val itemsCache = SalesCache()

    override fun getPage(requestParams: SalesParams): Single<DataPage<Sale>> {
        var salesPage: DataPage<Sale>? = null

        return apiProvider.getApi()
                .sales
                .getAll(requestParams)
                .toSingle()
                .map { page ->
                    salesPage = page
                    page.items.map { it.ownerAccount }
                }
                .flatMap { ownerAccounts ->
                    accountDetailsRepository
                            ?.getDetails(ownerAccounts)
                            ?.onErrorReturnItem(emptyMap())
                            ?: Single.just(emptyMap())
                }
                .map { owners ->
                    salesPage!!.items.forEach {
                        it.ownerDetails = owners[it.ownerAccount]
                    }

                    salesPage
                }
    }

    fun getSingle(id: Long): Single<Sale> {
        return apiProvider.getApi()
                .sales
                .getById(id)
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

    override fun getNextPageRequestParams(): SalesParams {

        return SalesParams(
                name = name,
                baseAsset = baseAsset,
                pagingParams = PagingParamsV2(
                        page = nextCursor,
                        order = PagingOrder.DESC,
                        limit = DEFAULT_LIMIT
                ),
                openOnly = true
        )
    }

    fun forQuery(name: String? = null, baseAsset: String? = null): SalesRepository {
        this.name = name
        this.baseAsset = baseAsset
        return this
    }

    companion object {
        private const val DEFAULT_LIMIT = 10
    }
}