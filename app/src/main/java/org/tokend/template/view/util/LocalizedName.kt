package org.tokend.template.view.util

import android.content.Context
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChangeAction
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.features.fees.view.FeeItem
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

    fun forBalanceChangeAction(action: BalanceChangeAction): String {
        return when (action) {
            BalanceChangeAction.LOCKED -> context.getString(R.string.tx_action_locked)
            BalanceChangeAction.UNLOCKED -> context.getString(R.string.tx_action_unlocked)
            BalanceChangeAction.CHARGED, BalanceChangeAction.CHARGED_FROM_LOCKED -> context.getString(R.string.tx_action_charged)
            BalanceChangeAction.WITHDRAWN -> context.getString(R.string.tx_action_withdrawn)
            BalanceChangeAction.MATCHED -> context.getString(R.string.tx_action_matched)
            BalanceChangeAction.ISSUED -> context.getString(R.string.tx_action_issued)
            BalanceChangeAction.FUNDED -> context.getString(R.string.tx_action_received)
        }
    }

    fun forBalanceChangeCause(cause: BalanceChangeCause): String {
        return when (cause) {
            is BalanceChangeCause.AmlAlert -> context.getString(R.string.balance_change_cause_aml)
            is BalanceChangeCause.Investment -> context.getString(R.string.balance_change_cause_investment)
            is BalanceChangeCause.MatchedOffer -> context.getString(R.string.balance_change_cause_matched_offer)
            is BalanceChangeCause.Issuance -> context.getString(R.string.balance_change_cause_issuance)
            is BalanceChangeCause.Payment -> context.getString(R.string.balance_change_cause_payment)
            is BalanceChangeCause.Withdrawal -> context.getString(R.string.balance_change_cause_withdrawal)
            is BalanceChangeCause.Offer -> {
                if (cause.isInvestment)
                    context.getString(R.string.balance_change_cause_pending_investment)
                else
                    context.getString(R.string.balance_change_cause_pending_offer)

            }
            is BalanceChangeCause.SaleCancellation ->
                context.getString(R.string.balance_change_cause_sale_cancellation)
            is BalanceChangeCause.OfferCancellation ->
                context.getString(R.string.balance_change_cause_offer_cancellation)
            is BalanceChangeCause.AssetPairUpdate ->
                context.getString(R.string.balance_change_cause_asset_pair_update)
            BalanceChangeCause.Unknown -> context.getString(R.string.balance_change_cause_unknown)
        }
    }
}