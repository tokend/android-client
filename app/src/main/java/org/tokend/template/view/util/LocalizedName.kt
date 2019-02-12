package org.tokend.template.view.util

import android.content.Context
import org.tokend.template.R
import org.tokend.template.features.fees.adapter.FeeItem
import org.tokend.wallet.xdr.FeeType

/**
 * Holds localized name getters for enums
 */
class LocalizedName(private val context: Context) {
    fun forFeeType(type: FeeType): String {
        return when (type) {
            FeeType.PAYMENT_FEE -> context.getString(R.string.payment_fee)
            FeeType.OFFER_FEE -> context.getString(R.string.offer_fee)
            FeeType.WITHDRAWAL_FEE -> context.getString(R.string.withdrawal_fee)
            FeeType.ISSUANCE_FEE -> context.getString(R.string.issuance_fee)
            FeeType.INVEST_FEE -> context.getString(R.string.invest_fee)
            FeeType.CAPITAL_DEPLOYMENT_FEE -> context.getString(R.string.capital_deployment_fee)
            FeeType.OPERATION_FEE -> context.getString(R.string.operation_fee)
            FeeType.PAYOUT_FEE -> context.getString(R.string.payout_fee)
            FeeType.ATOMIC_SWAP_SALE_FEE -> context.getString(R.string.atomic_swap_sale_fee)
            FeeType.ATOMIC_SWAP_PURCHASE_FEE -> context.getString(R.string.atomic_swap_purchase_fee)
        }
    }

    fun forFeeSubtype(subtype: FeeItem.Subtype): String {
        return when (subtype) {
            FeeItem.Subtype.INCOMING_OUTGOING -> context.getString(R.string.incoming_outgoing_fee)
            FeeItem.Subtype.OUTGOING -> context.getString(R.string.outgoing_fee)
            FeeItem.Subtype.INCOMING -> context.getString(R.string.incoming_fee)
        }
    }
}