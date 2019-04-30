package org.tokend.template.features.offers.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import org.tokend.template.features.offers.model.OfferRecord
import org.tokend.template.data.model.history.SimpleFeeRecord
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.logic.FeeManager

/**
 * Performs pre-confirmation offer preparations: loads fee.
 */
class PrepareOfferUseCase(
        private val offer: OfferRecord,
        private val walletInfoProvider: WalletInfoProvider,
        private val feeManager: FeeManager
) {
    private lateinit var accountId: String

    fun perform(): Single<OfferRecord> {
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

    private fun getFee(): Single<SimpleFeeRecord> {
        return feeManager.getOfferFee(
                accountId,
                offer.quoteAssetCode,
                offer.quoteAmount
        )
    }
}