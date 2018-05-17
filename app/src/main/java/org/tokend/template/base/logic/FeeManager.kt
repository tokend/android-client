package org.tokend.template.base.logic

import io.reactivex.Single
import org.tokend.sdk.api.models.Fee
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.extensions.toSingle
import org.tokend.wallet.xdr.FeeType
import java.math.BigDecimal

class FeeManager(
        private val apiProvider: ApiProvider
) {
    fun getFee(type: Int, accountId: String, asset: String, amount: BigDecimal): Single<Fee> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi.getFee(type, accountId, asset,
                BigDecimalUtil.toPlainString(amount))
                .toSingle()
    }

    fun getPaymentFee(accountId: String, asset: String, amount: BigDecimal): Single<Fee> {
        return getFee(FeeType.PAYMENT_FEE.value, accountId, asset, amount)
    }

    fun getWithdrawalFee(accountId: String, asset: String, amount: BigDecimal): Single<Fee> {
        return getFee(FeeType.WITHDRAWAL_FEE.value, accountId, asset, amount)
    }

    fun getOfferFee(accountId: String, asset: String, amount: BigDecimal): Single<Fee> {
        return getFee(FeeType.OFFER_FEE.value, accountId, asset, amount)
    }
}