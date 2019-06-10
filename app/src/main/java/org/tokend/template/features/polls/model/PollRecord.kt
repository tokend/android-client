package org.tokend.template.features.polls.model

import org.tokend.sdk.api.generated.resources.PollResource

class PollRecord(
        val id: String,
        val ownerAccountId: String,
        val subject: String,
        val choices: List<String>,
        var currentChoice: Int?
) {
    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is PollRecord && other.id == this.id
    }

    companion object {
        fun fromResource(source: PollResource): PollRecord {
            val details = source.creatorDetails

            val subject = details.get("subject").asText()

            // TODO: Resolve
            val choices = (1..source.numberOfChoices).map { "Choice #$it" }

            return PollRecord(
                    id = source.id,
                    ownerAccountId = source.owner.id,
                    subject = subject,
                    choices = choices,
                    currentChoice = null
            )
        }
    }
}