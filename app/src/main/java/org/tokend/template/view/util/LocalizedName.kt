package org.tokend.template.view.util

import android.content.Context
import org.tokend.template.R
import org.tokend.template.features.fees.adapter.FeeItem
import org.tokend.template.features.wallet.adapter.BalanceChangeListItem
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

    fun forBalanceChangeListItemAction(action: BalanceChangeListItem.Action): String {
        return when (action) {
            BalanceChangeListItem.Action.LOCKED -> context.getString(R.string.tx_action_locked)
            BalanceChangeListItem.Action.UNLOCKED -> context.getString(R.string.tx_action_unlocked)
            BalanceChangeListItem.Action.WITHDRAWN -> context.getString(R.string.tx_action_withdrawn)
            BalanceChangeListItem.Action.MATCHED -> context.getString(R.string.tx_action_matched)
            BalanceChangeListItem.Action.ISSUED -> context.getString(R.string.tx_action_issued)
            BalanceChangeListItem.Action.RECEIVED -> context.getString(R.string.tx_action_received)
            BalanceChangeListItem.Action.SENT -> context.getString(R.string.tx_action_sent)
            BalanceChangeListItem.Action.CHARGED -> context.getString(R.string.tx_action_charged)
        }
    }
}