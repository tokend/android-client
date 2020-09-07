package org.tokend.template.features.offers.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import org.tokend.template.features.assets.model.Asset
import org.tokend.template.features.history.model.SimpleFeeRecord
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.features.offers.model.OfferRecord
import org.tokend.template.features.offers.model.OfferRequest
import org.tokend.template.features.fees.logic.FeeManager
import java.math.BigDecimal

/**
 * Creates [OfferRequest]: loads fee for given amount
 */
class CreateOfferRequestUseCase(
        private val baseAmount: BigDecimal,
        private val price: BigDecimal,
        private val baseAsset: Asset,
        private val quoteAsset: Asset,
        private val orderBookId: Long,
        private val isBuy: Boolean,
        private val offerToCancel: OfferRecord?,
        private val walletInfoProvider: WalletInfoProvider,
        private val feeManager: FeeManager
) {
    private val quoteAmount = baseAmount * price
    private lateinit var accountId: String
    private lateinit var fee: SimpleFeeRecord

    fun perform(): Single<OfferRequest> {
        return getAccountId()
                .doOnSuccess { accountId ->
                    this.accountId = accountId
                }
                .flatMap {
                    getFee()
                }
                .doOnSuccess { fee ->
                    this.fee = fee
                }
                .flatMap {
                    getOfferRequest()
                }
    }

    private fun getAccountId(): Single<String> {
        return walletInfoProvider
                .getWalletInfo()
                ?.accountId
                .toMaybe()
                .switchIfEmpty(
                        Single.error(
                                IllegalStateException("Missing wallet info")
                        )
                )
    }

    private fun getFee(): Single<SimpleFeeRecord> {
        return feeManager.getOfferFee(
                orderBookId,
                accountId,
                quoteAsset.code,
                quoteAmount
        )
    }

    private fun getOfferRequest(): Single<OfferRequest> {
        return Single.just(
                OfferRequest(
                        orderBookId = orderBookId,
                        quoteAsset = quoteAsset,
                        baseAsset = baseAsset,
                        price = price,
                        isBuy = isBuy,
                        fee = fee,
                        baseAmount = baseAmount,
                        offerToCancel = offerToCancel
                )
        )
    }
}