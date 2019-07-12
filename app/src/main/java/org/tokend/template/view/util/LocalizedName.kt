package org.tokend.template.view.util

import android.content.Context
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChangeAction
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.features.fees.adapter.FeeListItem
import org.tokend.template.features.kyc.model.form.KycFormType
import org.tokend.template.features.wallet.adapter.BalanceChangeListItem
import org.tokend.template.view.assetchart.AssetChartScale
import org.tokend.wallet.xdr.FeeType
import org.tokend.wallet.xdr.StatsOpType
import java.util.*
import java.util.concurrent.TimeUnit

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
            else -> context.getString(R.string.balance_change_cause_unknown)
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

    fun forKycFormType(kycFormType: KycFormType): String {
        return when (kycFormType) {
            KycFormType.GENERAL -> context.getString(R.string.kyc_form_type_general)
            KycFormType.CORPORATE -> context.getString(R.string.kyc_form_type_corporate)
            KycFormType.UNKNOWN -> context.getString(R.string.kyc_form_type_unknown)
        }
    }

    fun forTimeUnit(unit: TimeUnit, value: Int): String {
        return when (unit) {
            TimeUnit.DAYS -> context.resources.getQuantityString(R.plurals.day, value)
            TimeUnit.HOURS -> context.resources.getQuantityString(R.plurals.hour, value)
            TimeUnit.MINUTES -> context.resources.getQuantityString(R.plurals.minute, value)
            else -> unit.name.toLowerCase()
        }
    }

    fun forLocale(locale: Locale): String {
        return locale.getDisplayLanguage(locale).toLowerCase().capitalize()
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
        }).first().toString().toUpperCase()
    }

    fun forMonth(numberFromZero: Int): String {
        return context.resources.getStringArray(R.array.months)[numberFromZero]
    }
}