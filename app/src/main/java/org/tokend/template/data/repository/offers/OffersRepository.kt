package org.tokend.template.data.repository.offers

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.accounts.params.OffersParams
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParams
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.data.repository.SystemInfoRepository
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.pagination.PagedDataRepository
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.extensions.toSingle
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
) : PagedDataRepository<OfferRecord, OffersParams>(itemsCache) {

    override fun getNextPageRequestParams(): OffersParams {
        return OffersParams(
                onlyPrimary = onlyPrimary,
                orderBookId = if (onlyPrimary) null else 0L,
                baseAsset = null,
                quoteAsset = null,
                isBuy = if (onlyPrimary) true else null,
                pagingParams = PagingParams(
                        cursor = nextCursor,
                        order = PagingOrder.DESC
                )
        )
    }

    override fun getItems(): Single<List<OfferRecord>> = Single.just(emptyList())

    override fun getPage(requestParams: OffersParams): Single<DataPage<OfferRecord>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))

        return signedApi.accounts.getPendingOffers(
                accountId = accountId,
                offersParams = requestParams
        )
                .toSingle()
                .map {
                    DataPage(
                            it.nextCursor,
                            it.items.map { offer ->
                                OfferRecord(offer)
                            },
                            it.isLast
                    )
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