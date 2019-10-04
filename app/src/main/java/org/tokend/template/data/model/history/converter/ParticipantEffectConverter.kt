package org.tokend.template.data.model.history.converter

import org.tokend.sdk.api.generated.resources.ParticipantsEffectResource
import org.tokend.template.data.model.history.BalanceChange

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