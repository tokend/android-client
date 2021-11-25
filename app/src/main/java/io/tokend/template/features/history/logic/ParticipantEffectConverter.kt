package io.tokend.template.features.history.logic

import io.tokend.template.features.history.model.BalanceChange
import org.tokend.sdk.api.v3.model.generated.resources.ParticipantsEffectResource

/**
 * Converts list of [ParticipantEffectResource] to the list of [BalanceChange]
 */
interface ParticipantEffectConverter {
    /**
     * Converts list of [ParticipantEffectResource] to the list of [BalanceChange]
     *
     * Notice that result list size may be different
     */
    fun toBalanceChanges(participantEffects: Collection<ParticipantsEffectResource>)
            : Collection<BalanceChange>
}