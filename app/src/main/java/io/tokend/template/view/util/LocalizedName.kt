package io.tokend.template.view.util

import android.content.Context
import io.tokend.template.R
import io.tokend.template.features.account.data.model.AccountRole
import io.tokend.template.features.fees.adapter.FeeListItem
import io.tokend.template.features.history.model.BalanceChangeAction
import io.tokend.template.features.history.model.details.BalanceChangeCause
import io.tokend.template.features.history.view.adapter.BalanceChangeListItem
import io.tokend.template.view.assetchart.AssetChartScale
import org.tokend.wallet.xdr.FeeType
import org.tokend.wallet.xdr.StatsOpType
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

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
            FeeType.SWAP_FEE -> context.getString(R.string.swap_fee)
        }
    }

    fun forFeeSubtype(subtype: FeeListItem.Subtype): String {
        return when (subtype) {
            FeeListItem.Subtype.INCOMING_OUTGOING -> context.getString(R.string.incoming_outgoing_fee)
            FeeListItem.Subtype.OUTGOING -> context.getString(R.string.outgoing_fee)
            FeeListItem.Subtype.INCOMING -> context.getString(R.string.incoming_fee)
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
            BalanceChangeAction.CHARGED, BalanceChangeAction.CHARGED_FROM_LOCKED -> context.getString(
                R.string.tx_action_charged
            )
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
            is BalanceChangeCause.WithdrawalRequest -> context.getString(R.string.balance_change_cause_withdrawal_request)
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
            is BalanceChangeCause.AtomicSwapAskCreation ->
                context.getString(R.string.balance_change_cause_aswap_ask_creation)
            is BalanceChangeCause.AtomicSwapBidCreation ->
                context.getString(R.string.balance_change_cause_aswap_bid_creation)
            else -> context.getString(R.string.unknown_balance_change)
        }
    }

    fun forLimitType(limitType: StatsOpType): String {
        return when (limitType) {
            StatsOpType.PAYMENT_OUT -> context.getString(R.string.payment)
            StatsOpType.WITHDRAW -> context.getString(R.string.withdraw_title)
            StatsOpType.DEPOSIT -> context.getString(R.string.deposit)
            StatsOpType.SPEND -> context.getString(R.string.spend)
            StatsOpType.PAYOUT -> context.getString(R.string.payout)
        }
    }

    fun forAccountRole(accountRole: AccountRole): String {
        return when (accountRole) {
            AccountRole.UNVERIFIED -> context.getString(R.string.account_role_unverified)
            AccountRole.GENERAL -> context.getString(R.string.account_role_general)
            AccountRole.CORPORATE -> context.getString(R.string.account_role_corporate)
            AccountRole.BLOCKED -> context.getString(R.string.account_role_blocked)
            AccountRole.UNKNOWN -> context.getString(R.string.account_role_unknown)
        }
    }

    fun forTimeUnit(unit: TimeUnit, value: Int): String {
        val absoluteValue = value.absoluteValue
        return when (unit) {
            TimeUnit.DAYS -> context.resources.getQuantityString(R.plurals.day, absoluteValue)
            TimeUnit.HOURS -> context.resources.getQuantityString(R.plurals.hour, absoluteValue)
            TimeUnit.MINUTES -> context.resources.getQuantityString(R.plurals.minute, absoluteValue)
            else -> unit.name.toLowerCase(Locale.getDefault())
        }
    }

    fun forLocale(locale: Locale): String {
        return locale.getDisplayLanguage(locale).toLowerCase(locale).capitalize(locale)
    }

    fun forAssetChartScaleLast(scale: AssetChartScale): String {
        return when (scale) {
            AssetChartScale.HOUR -> context.getString(R.string.last_hour)
            AssetChartScale.DAY -> context.getString(R.string.last_day)
            AssetChartScale.MONTH -> context.getString(R.string.last_month)
            AssetChartScale.YEAR -> context.getString(R.string.last_year)
        }
    }

    fun forAssetChartScaleShort(scale: AssetChartScale): String {
        return (when (scale) {
            AssetChartScale.HOUR -> context.getString(R.string.hour)
            AssetChartScale.DAY -> context.getString(R.string.day)
            AssetChartScale.MONTH -> context.getString(R.string.month)
            AssetChartScale.YEAR -> context.getString(R.string.year)
        }).first().toString().toUpperCase(Locale.getDefault())
    }

    fun forMonth(numberFromZero: Int): String {
        return context.resources.getStringArray(R.array.months)[numberFromZero]
    }
}