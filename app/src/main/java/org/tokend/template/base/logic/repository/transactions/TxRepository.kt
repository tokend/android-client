package org.tokend.template.base.logic.repository.transactions

import io.reactivex.Single
import org.tokend.sdk.api.accounts.params.PaymentsParams
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.model.transactions.PaymentTransaction
import org.tokend.sdk.api.base.model.transactions.Transaction
import org.tokend.sdk.api.base.params.OperationsParams
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParams
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.repository.AccountDetailsRepository
import org.tokend.template.base.logic.repository.base.pagination.PagedDataRepository
import org.tokend.template.extensions.toSingle

class TxRepository(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val asset: String,
        private val accountDetailsRepository: AccountDetailsRepository? = null
) : PagedDataRepository<Transaction, PaymentsParams>() {

    override val itemsCache = TxCache()

    override fun getItems(): Single<List<Transaction>> = Single.just(emptyList())

    override fun getPage(requestParams: PaymentsParams): Single<DataPage<Transaction>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))
        val accountsToLoad = mutableListOf<String>()

        return signedApi
                .accounts
                .getPaymentTransactions(
                        accountId,
                        requestParams
                )
                .toSingle()
                .doOnSuccess { transactionsPage ->
                    transactionsPage.items.forEach { transaction ->
                        if (transaction is PaymentTransaction) {
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
                                    if (it is PaymentTransaction) {
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