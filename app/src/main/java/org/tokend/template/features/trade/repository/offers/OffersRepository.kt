package org.tokend.template.features.trade.repository.offers

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.models.Offer
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.repository.SystemInfoRepository
import org.tokend.template.base.logic.repository.base.pagination.DataPage
import org.tokend.template.base.logic.repository.base.pagination.PageParams
import org.tokend.template.base.logic.repository.base.pagination.PagedDataRepository
import org.tokend.template.base.logic.repository.base.pagination.PagedRequestParams
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.template.extensions.toSingle
import org.tokend.wallet.Account
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.Transaction
import org.tokend.wallet.xdr.ManageOfferOp
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.op_extensions.CancelOfferOp
import java.math.BigDecimal

class OffersRepository(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider
) : PagedDataRepository<Offer, OffersRepository.OffersRequestParams>() {
    class OffersRequestParams(val onlyPrimaryMarket: Boolean,
                              val orderBookId: Long,
                              val baseAsset: String?,
                              val quoteAsset: String?,
                              val isBuy: Boolean?,
                              pageParams: PageParams = PageParams()
    ) : PagedRequestParams(pageParams)

    override val itemsCache = OffersCache()

    override fun getNextPageRequestParams(): OffersRequestParams {
        return OffersRequestParams(true,
                0,
                null,
                null,
                null,
                PageParams(cursor = nextCursor))
    }

    override fun getItems(): Single<List<Offer>> = Single.just(emptyList())

    override fun getPage(requestParams: OffersRequestParams): Single<DataPage<Offer>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))

        return signedApi.getOffers(
                accountId = accountId,
                baseAsset = requestParams.baseAsset,
                quoteAsset = requestParams.quoteAsset,
                isBuy = requestParams.isBuy,
                limit = requestParams.pageParams.limit,
                cursor = requestParams.pageParams.cursor,
                orderBookId = requestParams.orderBookId,
                order = "desc"
        )
                .toSingle()
                .map {
                    DataPage.fromPage(it)
                }
    }

    // region Create.
    fun create(accountProvider: AccountProvider,
               systemInfoRepository: SystemInfoRepository,
               txManager: TxManager,
               offer: Offer): Completable {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Completable.error(IllegalStateException("No wallet info found"))
        val account = accountProvider.getAccount()
                ?: return Completable.error(IllegalStateException("Cannot obtain current account"))

        return systemInfoRepository.getNetworkParams()
                .flatMap { netParams ->
                    createOfferCreationTransaction(netParams, accountId, account, offer)
                }
                .flatMap { transition ->
                    txManager.submit(transition)
                }
                .toCompletable()
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnTerminate {
                    isLoading = false
                }
                .doOnComplete {
                    invalidate()
                }
    }

    private fun createOfferCreationTransaction(networkParams: NetworkParams,
                                               sourceAccountId: String,
                                               signer: Account,
                                               offer: Offer): Single<Transaction> {
        return {
            ManageOfferOp(
                    baseBalance = PublicKeyFactory.fromBalanceId(offer.baseBalance ?: ""),
                    quoteBalance = PublicKeyFactory.fromBalanceId(offer.quoteBalance ?: ""),
                    amount = networkParams.amountToPrecised(offer.baseAmount),
                    price = networkParams.amountToPrecised(offer.price),
                    fee = networkParams.amountToPrecised(offer.fee ?: BigDecimal.ZERO),
                    isBuy = offer.isBuy,
                    orderBookID = 0L,
                    offerID = 0L,
                    ext = ManageOfferOp.ManageOfferOpExt.EmptyVersion()
            )
        }
                .toSingle()
                .subscribeOn(Schedulers.computation())
                .map { Operation.OperationBody.ManageOffer(it) }
                .flatMap {
                    TxManager.createSignedTransaction(networkParams, sourceAccountId, signer, it)
                }
    }
    // endregion

    // region Cancel
    fun cancel(accountProvider: AccountProvider,
               systemInfoRepository: SystemInfoRepository,
               txManager: TxManager,
               offer: Offer): Completable {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Completable.error(IllegalStateException("No wallet info found"))
        val account = accountProvider.getAccount()
                ?: return Completable.error(IllegalStateException("Cannot obtain current account"))

        return systemInfoRepository.getNetworkParams()
                .flatMap { netParams ->
                    createOfferCancellationTransaction(netParams, accountId, account, offer)
                }
                .flatMap { transition ->
                    txManager.submit(transition)
                }
                .toCompletable()
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnTerminate {
                    isLoading = false
                }
                .doOnComplete {
                    itemsCache.merge(emptyList()) {
                        it.id == offer.id
                    }
                    broadcast()
                }
    }

    private fun createOfferCancellationTransaction(networkParams: NetworkParams,
                                                   sourceAccountId: String,
                                                   signer: Account,
                                                   offer: Offer): Single<Transaction> {
        return {
            CancelOfferOp(offer.id, offer.isBuy)
        }
                .toSingle()
                .subscribeOn(Schedulers.computation())
                .map { Operation.OperationBody.ManageOffer(it) }
                .flatMap {
                    TxManager.createSignedTransaction(networkParams, sourceAccountId, signer, it)
                }
    }
    // endregion
}