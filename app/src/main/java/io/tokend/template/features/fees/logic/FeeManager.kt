package io.tokend.template.features.fees.logic

import io.reactivex.Single
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.features.history.model.SimpleFeeRecord
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.v3.fees.params.FeeCalculationParams
import org.tokend.wallet.xdr.FeeType
import org.tokend.wallet.xdr.PaymentFeeType
import java.math.BigDecimal

/**
 * Manages operation fees loading
 */
class FeeManager(
    private val apiProvider: ApiProvider
) {
    /**
     * @return fee for given operation params
     *
     * @param type [FeeType] value
     * @param subtype fee subtype, use 0 if there is no subtypes for required operation
     * @param accountId ID of the fee payer account
     * @param amount operation amount
     * @param asset operation asset
     */
    fun getFee(
        type: FeeType, subtype: Int,
        accountId: String, asset: String, amount: BigDecimal
    ): Single<SimpleFeeRecord> {
        val signedApi = apiProvider.getSignedApi()

        val params = FeeCalculationParams(
            asset = asset,
            type = type,
            subtype = subtype,
            amount = amount
        )

        return signedApi.v3.fees
            .getCalculatedFee(accountId, params)
            .toSingle()
            .map(::SimpleFeeRecord)
    }

    /**
     * @return payment fee for given params
     *
     * @param isOutgoing controls subtype of payment fee
     *
     * @see PaymentFeeType
     * @see getFee
     */
    fun getPaymentFee(
        accountId: String, asset: String, amount: BigDecimal,
        isOutgoing: Boolean
    ): Single<SimpleFeeRecord> {
        val subtype =
            if (isOutgoing)
                PaymentFeeType.OUTGOING.value
            else
                PaymentFeeType.INCOMING.value
        return getFee(FeeType.PAYMENT_FEE, subtype, accountId, asset, amount)
    }

    /**
     * @return withdrawal fee for given params
     *
     * @see getFee
     */
    fun getWithdrawalFee(
        accountId: String,
        asset: String,
        amount: BigDecimal
    ): Single<SimpleFeeRecord> {
        return getFee(FeeType.WITHDRAWAL_FEE, 0, accountId, asset, amount)
    }

    /**
     * @return offer fee for given params
     *
     * @see getFee
     */
    fun getOfferFee(
        orderBookId: Long,
        accountId: String,
        asset: String,
        amount: BigDecimal
    ): Single<SimpleFeeRecord> {
        val feeType = if (orderBookId != 0L) {
            FeeType.INVEST_FEE
        } else FeeType.OFFER_FEE

        return getFee(feeType, 0, accountId, asset, amount)
    }
}