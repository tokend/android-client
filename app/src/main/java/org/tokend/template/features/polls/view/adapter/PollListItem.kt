package org.tokend.template.features.polls.view.adapter

import org.tokend.template.features.polls.model.PollRecord

class PollListItem(
        val subject: String,
        val choices: List<PollRecord.Choice>,
        val currentChoice: Int?,
        val canVote: Boolean,
        val hasResults: Boolean,
        val source: PollRecord?
) {
    constructor(source: PollRecord) : this(
            subject = source.subject,
            choices = source.choices.toList(),
            currentChoice = source.currentChoice,
            canVote = source.canVote,
            hasResults = source.hasResults,
            source = source
    )
}