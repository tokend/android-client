package org.tokend.template.data.repository.offers

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.data.repository.SystemInfoRepository
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.pagination.PagedDataRepository
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.offers.params.OffersPageParamsV3
import org.tokend.sdk.utils.SimplePagedResourceLoader
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.Account
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.Transaction
import org.tokend.wallet.xdr.ManageOfferOp
import org.tokend.wallet.xdr.Operation

class OffersRepository(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val onlyPrimary: Boolean,
        itemsCache: RepositoryCache<OfferRecord>
) : PagedDataRepository<OfferRecord>(itemsCache) {
    override fun getPage(nextCursor: String?): Single<DataPage<OfferRecord>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))

        val requestParams = OffersPageParamsV3(
                ownerAccount = accountId,
                orderBook = if (onlyPrimary) null else 0L,
                baseAsset = null,
                quoteAsset = null,
                isBuy = if (onlyPrimary) true else null,
                pagingParams = PagingParamsV2(
                        page = nextCursor,
                        order = PagingOrder.DESC
                )
        )

        return signedApi.v3.offers.get(requestParams)
                .toSingle()
                .map {
                    val items = it.items.map { offerResource ->
                        OfferRecord.fromResource(offerResource)
                    }.let { offers ->
                        if (onlyPrimary) {
                            offers.filter { record ->
                                record.orderBookId != 0L
                            }
                        } else offers
                    }

                    DataPage(
                            it.nextCursor,
                            items,
                            it.isLast
                    )
                }
    }

    fun getForSale(saleId: Long): Single<List<OfferRecord>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))

        val loader = SimplePagedResourceLoader({ nextCursor ->
            signedApi.v3.offers.get(
                    OffersPageParamsV3(
                            ownerAccount = accountId,
                            orderBook = saleId,
                            pagingParams = PagingParamsV2(page = nextCursor)
                    )
            )
        })

        return loader.loadAll()
                .toSingle()
                .map {
                    it.map { OfferRecord.fromResource(it) }
                }
    }

    // region Create.
    /**
     * Submits given offer,
     * triggers repository update on complete
     */
    fun create(accountProvider: AccountProvider,
               systemInfoRepository: SystemInfoRepository,
               txManager: TxManager,
               offer: OfferRecord,
               offerToCancel: OfferRecord? = null): Completable {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Completable.error(IllegalStateException("No wallet info found"))
        val account = accountProvider.getAccount()
                ?: return Completable.error(IllegalStateException("Cannot obtain current account"))

        return systemInfoRepository.getNetworkParams()
                .flatMap { netParams ->
                    createOfferCreationTransaction(netParams, accountId, account,
                            offer, offerToCancel)
                }
                .flatMap { transition ->
                    txManager.submit(transition)
                }
                .ignoreElement()
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnTerminate {
                    isLoading = false
                }
                .doOnComplete {
                    update()
                }
    }

    private fun createOfferCreationTransaction(networkParams: NetworkParams,
                                               sourceAccountId: String,
                                               signer: Account,
                                               offer: OfferRecord,
                                               offerToCancel: OfferRecord?): Single<Transaction> {
        return offerToCancel
                .toMaybe()
                .toObservable()
                .map<ManageOfferOp> {
                    ManageOfferOp(
                            baseBalance =
                            PublicKeyFactory.fromBalanceId(it.baseBalanceId),
                            quoteBalance =
                            PublicKeyFactory.fromBalanceId(it.quoteBalanceId),
                            amount = 0,
                            price = networkParams.amountToPrecised(it.price),
                            fee = networkParams.amountToPrecised(it.fee),
                            isBuy = it.isBuy,
                            orderBookID = it.orderBookId,
                            offerID = it.id,
                            ext = ManageOfferOp.ManageOfferOpExt.EmptyVersion()
                    )
                }
                .concatWith(
                        Observable.just(
                                ManageOfferOp(
                                        baseBalance =
                                        PublicKeyFactory.fromBalanceId(offer.baseBalanceId),
                                        quoteBalance =
                                        PublicKeyFactory.fromBalanceId(offer.quoteBalanceId),
                                        amount = networkParams.amountToPrecised(offer.baseAmount),
                                        price = networkParams.amountToPrecised(offer.price),
                                        fee = networkParams.amountToPrecised(offer.fee),
                                        isBuy = offer.isBuy,
                                        orderBookID = offer.orderBookId,
                                        offerID = 0L,
                                        ext = ManageOfferOp.ManageOfferOpExt.EmptyVersion()
                                )
                        )
                )
                .subscribeOn(Schedulers.newThread())
                .map { Operation.OperationBody.ManageOffer(it) }
                .toList()
                .map { it.toTypedArray() }
                .flatMap { operations ->
                    TxManager.createSignedTransaction(networkParams, sourceAccountId, signer,
                            *operations)
                }
    }
    // endregion

    // region Cancel
    /**
     * Cancels given offer,
     * locally removes it from repository on complete
     */
    fun cancel(accountProvider: AccountProvider,
               systemInfoRepository: SystemInfoRepository,
               txManager: TxManager,
               offer: OfferRecord): Completable {
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
                .ignoreElement()
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnTerminate {
                    isLoading = false
                }
                .doOnComplete {
                    itemsCache.transform(emptyList()) {
                        it.id == offer.id
                    }
                    broadcast()
                }
    }

    private fun createOfferCancellationTransaction(networkParams: NetworkParams,
                                                   sourceAccountId: String,
                                                   signer: Account,
                                                   offer: OfferRecord): Single<Transaction> {
        return {
            ManageOfferOp(
                    baseBalance =
                    PublicKeyFactory.fromBalanceId(offer.baseBalanceId),
                    quoteBalance =
                    PublicKeyFactory.fromBalanceId(offer.quoteBalanceId),
                    amount = 0,
                    price = networkParams.amountToPrecised(offer.price),
                    fee = networkParams.amountToPrecised(offer.fee),
                    isBuy = offer.isBuy,
                    orderBookID = offer.orderBookId,
                    offerID = offer.id,
                    ext = ManageOfferOp.ManageOfferOpExt.EmptyVersion()
            )
        }
                .toSingle()
                .subscribeOn(Schedulers.newThread())
                .map { Operation.OperationBody.ManageOffer(it) }
                .flatMap {
                    TxManager.createSignedTransaction(networkParams, sourceAccountId, signer, it)
                }
    }
    // endregion
}