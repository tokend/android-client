package org.tokend.template.data.model.history.converter

import android.util.Log
import org.tokend.sdk.api.generated.resources.*
import org.tokend.template.data.model.SimpleAsset
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.BalanceChangeAction
import org.tokend.template.data.model.history.SimpleFeeRecord
import org.tokend.template.data.model.history.details.BalanceChangeCause

class DefaultParticipantEffectConverter: ParticipantEffectConverter {
    override fun toBalanceChanges(participantEffects: Collection<ParticipantsEffectResource>)
            : Collection<BalanceChange> {
        val result = ArrayList<BalanceChange>(participantEffects.size)

        participantEffects.forEach { participantEffect ->
            val operation = participantEffect.operation

            if (operation == null) {
                logError("No related operation for participant effect ${participantEffect.id}")
                return@forEach
            }

            if (!operation.isFilled) {
                logError("Operation ${operation.id} attributes are not filled")
                return@forEach
            }

            val date = operation.appliedAt

            val effect = participantEffect.effect

            if (effect == null) {
                logError("No related effect for participant effect ${participantEffect.id}")
                return@forEach
            }

            val balanceId = participantEffect.balance?.id

            if (balanceId == null) {
                logError("No related balance for participant effect ${participantEffect.id}")
                return@forEach
            }

            val action: BalanceChangeAction = when (effect) {
                is EffectsLockedResource ->
                    BalanceChangeAction.LOCKED
                is EffectsChargedFromLockedResource ->
                    BalanceChangeAction.CHARGED_FROM_LOCKED
                is EffectsUnlockedResource ->
                    BalanceChangeAction.UNLOCKED
                is EffectsChargedResource ->
                    BalanceChangeAction.CHARGED
                is EffectsWithdrawnResource ->
                    BalanceChangeAction.WITHDRAWN
                is EffectMatchedResource ->
                    BalanceChangeAction.MATCHED
                is EffectsIssuedResource ->
                    BalanceChangeAction.ISSUED
                is EffectsFundedResource ->
                    BalanceChangeAction.FUNDED
                else -> {
                    logError("Cannot obtain action from effect ${effect.id} $effect")
                    return@forEach
                }
            }

            val relatedAssetCode = participantEffect.asset?.id

            val (amount, fee, assetCode) = when (effect) {
                is EffectBalanceChangeResource ->
                    Triple(effect.amount, effect.fee, relatedAssetCode)
                is EffectMatchedResource -> {
                    when (balanceId) {
                        effect.charged.balanceAddress ->
                            effect.charged.let {
                                Triple(it.amount, it.fee, it.assetCode)
                            }
                        effect.funded.balanceAddress ->
                            effect.funded.let {
                                Triple(it.amount, it.fee, it.assetCode)
                            }
                        else -> {
                            logError("Cannot choose 'funded' or 'charged' " +
                                    "for balance $balanceId and effect ${effect.id}")
                            return@forEach
                        }
                    }
                }
                else -> {
                    logError("Cannot obtain amount and fee from effect ${effect.id} $effect")
                    return@forEach
                }
            }

            if (assetCode == null) {
                logError("Failed to specify asset of participant effect ${participantEffect.id} " +
                        "and balance $balanceId")
                return@forEach
            }

            val operationDetails = operation.details

            if (operationDetails == null) {
                logError("No related operation details for operation ${operation.id}")
                return@forEach
            }

            if (!operationDetails.isFilled) {
                logError("Operation ${operationDetails.id} attributes are not filled")
                return@forEach
            }

            val cause = getCause(effect, operationDetails)

            result.add(
                    BalanceChange(
                            id = effect.id,
                            action = action,
                            amount = amount,
                            fee = SimpleFeeRecord(fee),
                            asset = SimpleAsset(assetCode),
                            balanceId = balanceId,
                            date = date,
                            cause = cause
                    )
            )
        }

        return result
    }

    private fun getCause(effect: BaseEffectResource,
                         operationDetails: BaseOperationDetailsResource)
            : BalanceChangeCause {
        return try {
            when (operationDetails) {
                is PaymentOpResource ->
                    BalanceChangeCause.Payment(operationDetails)
                is CreateIssuanceRequestOpResource ->
                    BalanceChangeCause.Issuance(operationDetails)
                is CreateWithdrawRequestOpResource ->
                    BalanceChangeCause.WithdrawalRequest(operationDetails)
                is ManageOfferOpResource ->
                    when (effect) {
                        is EffectMatchedResource -> BalanceChangeCause.MatchedOffer(operationDetails, effect)
                        is EffectsUnlockedResource -> BalanceChangeCause.OfferCancellation
                        else -> BalanceChangeCause.Offer(operationDetails)
                    }
                is CheckSaleStateOpResource ->
                    when (effect) {
                        is EffectMatchedResource -> BalanceChangeCause.Investment(effect)
                        is EffectsIssuedResource -> BalanceChangeCause.Issuance(null, null)
                        is EffectsUnlockedResource -> BalanceChangeCause.SaleCancellation
                        else -> BalanceChangeCause.Unknown
                    }
                is CreateAmlAlertRequestOpResource ->
                    BalanceChangeCause.AmlAlert(operationDetails)
                is ManageAssetPairOpResource ->
                    BalanceChangeCause.AssetPairUpdate(operationDetails)
                is CreateAtomicSwapAskRequestOpResource ->
                    BalanceChangeCause.AtomicSwapAskCreation
                is CreateAtomicSwapBidRequestOpResource ->
                    BalanceChangeCause.AtomicSwapAskCreation
                else ->
                    BalanceChangeCause.Unknown
            }
        } catch (e: Exception) {
            logError("Unable to parse operation details ${operationDetails.id}: "
                    + e.localizedMessage)
            BalanceChangeCause.Unknown
        }
    }

    private fun logError(error: String) {
        Log.e(LOG_TAG, error)
    }

    companion object {
        private const val LOG_TAG = "DefaultEffectConverter"
    }
}