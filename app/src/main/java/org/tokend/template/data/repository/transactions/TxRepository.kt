package org.tokend.template.data.repository.transactions

import io.reactivex.Single
import org.tokend.sdk.api.accounts.params.PaymentsParams
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.model.operations.PaymentOperation
import org.tokend.sdk.api.base.model.operations.TransferOperation
import org.tokend.sdk.api.base.params.OperationsParams
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParams
import org.tokend.template.data.repository.AccountDetailsRepository
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.pagination.PagedDataRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.extensions.toSingle

class TxRepository(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val asset: String,
        private val accountDetailsRepository: AccountDetailsRepository? = null,
        itemsCache: RepositoryCache<TransferOperation>
) : PagedDataRepository<TransferOperation, PaymentsParams>(itemsCache) {

    override fun getItems(): Single<List<TransferOperation>> = Single.just(emptyList())

    override fun getPage(requestParams: PaymentsParams): Single<DataPage<TransferOperation>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))
        val accountsToLoad = mutableListOf<String>()

        return signedApi
                .accounts
                .getPayments(
                        accountId,
                        requestParams
                )
                .toSingle()
                .doOnSuccess { transactionsPage ->
                    transactionsPage.items.forEach { transaction ->
                        if (transaction is PaymentOperation) {
                            accountsToLoad.add(transaction.sourceAccount)
                            accountsToLoad.add(transaction.destAccount)
                        }
                    }
                }
                .flatMap { transactionsPage ->
                    accountDetailsRepository
                            ?.getDetails(accountsToLoad.toList())
                            ?.onErrorReturnItem(emptyMap())
                            ?.map { detailsMap ->
                                transactionsPage.items.forEach {
                                    if (it is PaymentOperation) {
                                        if (it.isSent) {
                                            it.counterpartyNickname =
                                                    detailsMap[it.destAccount]?.email
                                        } else {
                                            it.counterpartyNickname =
                                                    detailsMap[it.sourceAccount]?.email
                                        }
                                    }
                                }

                                transactionsPage
                            }
                            ?: Single.just(transactionsPage)
                }
    }

    override fun getNextPageRequestParams(): PaymentsParams {
        return PaymentsParams(
                asset = asset,
                operationsParams = OperationsParams(
                        completedOnly = false
                ),
                pagingParams = PagingParams(
                        cursor = nextCursor,
                        order = PagingOrder.DESC
                )
        )
    }
}