package org.tokend.template.logic

import io.reactivex.Single
import org.tokend.sdk.api.fees.model.Fee
import org.tokend.sdk.api.fees.params.FeeParams
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.extensions.toSingle
import org.tokend.wallet.xdr.FeeType
import org.tokend.wallet.xdr.PaymentFeeType
import java.math.BigDecimal

class FeeManager(
        private val apiProvider: ApiProvider
) {
    fun getFee(type: Int, subtype: Int,
               accountId: String, asset: String, amount: BigDecimal): Single<Fee> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi.fees.getByType(
                type,
                FeeParams(
                        asset,
                        accountId,
                        amount,
                        subtype)
        )
                .toSingle()
    }

    fun getPaymentFee(accountId: String, asset: String, amount: BigDecimal): Single<Fee> {
        return getFee(FeeType.PAYMENT_FEE.value, 0, accountId, asset, amount)
    }

    fun getPaymentFee(accountId: String, asset: String, amount: BigDecimal,
                      isOutgoing: Boolean): Single<Fee> {
        val subtype =
                if (isOutgoing)
                    PaymentFeeType.OUTGOING.value
                else
                    PaymentFeeType.INCOMING.value
        return getFee(FeeType.PAYMENT_FEE.value, subtype, accountId, asset, amount)
    }

    fun getWithdrawalFee(accountId: String, asset: String, amount: BigDecimal): Single<Fee> {
        return getFee(FeeType.WITHDRAWAL_FEE.value, 0, accountId, asset, amount)
    }

    fun getOfferFee(accountId: String, asset: String, amount: BigDecimal): Single<Fee> {
        return getFee(FeeType.OFFER_FEE.value, 0, accountId, asset, amount)
    }
}