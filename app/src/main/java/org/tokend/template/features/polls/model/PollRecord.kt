package org.tokend.template.features.polls.model

import org.tokend.sdk.api.generated.resources.PollResource

class PollRecord(
        val id: String,
        val ownerAccountId: String,
        val subject: String,
        val choices: List<String>,
        var currentChoice: Int?
) {
    constructor(source: PollResource): this(
            id = source.id,
            ownerAccountId = source.owner.id,
            subject = "Example question",
            choices = (1..source.numberOfChoices).map { "Choice #$it" },
            currentChoice = null
    )

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is PollRecord && other.id == this.id
    }
}