package org.tokend.template.features.polls.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.tokend.sdk.api.generated.resources.PollResource
import org.tokend.sdk.factory.JsonApiToolsProvider

class PollRecord(
        val id: String,
        val ownerAccountId: String,
        val subject: String,
        val choices: List<Choice>,
        var currentChoice: Int?
) {
    private class ChoiceData
    @JsonCreator
    constructor(
            @JsonProperty("number")
            val number: Int,
            @JsonProperty("description")
            val description: String
    )

    class Choice(
            val name: String,
            val result: Result?
    ) {
        class Result(
                val votesCount: Int,
                val percentOfTotal: Double
        ) {
            constructor(votesCount: Int, totalVotes: Long) : this(
                    votesCount = votesCount,
                    percentOfTotal = if (totalVotes != 0L)
                        (votesCount.toDouble() / totalVotes) * 100
                    else
                        0.toDouble()
            )
        }
    }

    val hasResults = choices.all { it.result != null }
    val canVote: Boolean
        get() = currentChoice == null

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is PollRecord && other.id == this.id
    }

    companion object {
        fun fromResource(source: PollResource): PollRecord {
            val details = source.creatorDetails

            val subject = details.get("question").asText()

            val mapper = JsonApiToolsProvider.getObjectMapper()
            val choicesData = details
                    .withArray("choices")
                    .map { mapper.treeToValue(it, ChoiceData::class.java) }
                    .sortedBy(ChoiceData::number)

            val choices = choicesData
                    .map {
                        Choice(
                                name = it.description,
                                result = null
                        )
                    }

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