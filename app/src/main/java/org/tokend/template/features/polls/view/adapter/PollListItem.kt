package org.tokend.template.features.polls.view.adapter

import org.tokend.template.features.polls.model.PollRecord
import java.util.*

class PollListItem(
        val subject: String,
        val choices: List<PollRecord.Choice>,
        val currentChoice: Int?,
        val canVote: Boolean,
        val isEnded: Boolean,
        val endDate: Date,
        val source: PollRecord?
) {
    constructor(source: PollRecord) : this(
            subject = source.subject,
            choices = source.choices.toList(),
            currentChoice = source.currentChoice,
            canVote = source.canVote,
            isEnded = source.isEnded,
            endDate = source.endDate,
            source = source
    )

    val canChoose = canVote && currentChoice == null
}