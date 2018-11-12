package org.tokend.template.features.trade

import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import org.tokend.sdk.api.fees.model.Fee
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.template.base.logic.FeeManager
import org.tokend.template.base.logic.di.providers.WalletInfoProvider

/**
 * Performs pre-confirmation offer preparations: loads fee.
 */
class PrepareOfferUseCase(
        private val offer: Offer,
        private val walletInfoProvider: WalletInfoProvider,
        private val feeManager: FeeManager
) {
    private lateinit var accountId: String

    fun perform(): Single<Offer> {
        return getAccountId()
                .doOnSuccess { accountId ->
                    this.accountId = accountId
                }
                .flatMap {
                    getFee()
                }
                .doOnSuccess { fee ->
                    offer.fee = fee.percent
                }
                .map {
                    offer
                }
    }

    private fun getAccountId(): Single<String> {
        return walletInfoProvider
                .getWalletInfo()
                ?.accountId
                .toMaybe()
                .switchIfEmpty(
                        Single.error<String>(
                                IllegalStateException("Missing wallet info")
                        )
                )
    }

    private fun getFee(): Single<Fee> {
        return feeManager.getOfferFee(
                accountId,
                offer.quoteAsset,
                offer.quoteAmount
        )
    }
}