package org.tokend.template.features.history.logic

import org.tokend.sdk.api.generated.resources.ParticipantsEffectResource
import org.tokend.template.features.history.model.BalanceChange

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