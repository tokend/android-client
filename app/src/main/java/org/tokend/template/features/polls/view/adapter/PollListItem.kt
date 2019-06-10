package org.tokend.template.features.polls.view.adapter

import org.tokend.template.features.polls.model.PollRecord

class PollListItem(
        val subject: String,
        val choices: List<String>,
        val currentChoice: Int?,
        val source: PollRecord?
) {
    constructor(source: PollRecord): this(
            subject = source.subject,
            choices = source.choices.toList(),
            currentChoice = source.currentChoice,
            source = source
    )
}