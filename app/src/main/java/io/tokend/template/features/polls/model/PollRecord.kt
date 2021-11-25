package io.tokend.template.features.polls.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.tokend.sdk.api.v3.model.generated.resources.PollResource
import org.tokend.sdk.api.v3.polls.model.PollState
import org.tokend.sdk.factory.JsonApiTools
import java.util.*

class PollRecord(
    val id: String,
    val ownerAccountId: String,
    val subject: String,
    val choices: List<Choice>,
    var currentChoice: Int?,
    val isEnded: Boolean,
    val isChoiceChangeAllowed: Boolean,
    val endDate: Date
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

    val canVote: Boolean
        get() = (currentChoice == null || isChoiceChangeAllowed) && !isEnded

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is PollRecord && other.id == this.id
    }

    companion object {
        /**
         * @param permissionTypeAllowChoiceChange key-value entry value
         * for [PollResource.permissionType] which allows choice change
         */
        fun fromResource(
            source: PollResource,
            permissionTypeAllowChoiceChange: Long?
        ): PollRecord {
            val details = source.creatorDetails

            val subject = details.get("question").asText()

            val mapper = JsonApiTools.objectMapper
            val choicesData = details["choices"]
                .map { mapper.treeToValue(it, ChoiceData::class.java) }
                .sortedBy(ChoiceData::number)

            val outcome = details["outcome"]
            val votesByChoices = choicesData
                .associateBy(ChoiceData::number)
                .mapValues { outcome?.get(it.key.toString())?.asInt() ?: 0 }
            val totalVotes = votesByChoices.values.map(Int::toLong).sum()

            val pollIsEnded = source.pollState.value == PollState.PASSED.value
                    || source.pollState.value == PollState.FAILED.value

            val choices = choicesData
                .map {
                    Choice(
                        name = it.description,
                        result =
                        if (pollIsEnded)
                            Choice.Result(
                                votesCount = votesByChoices[it.number] ?: 0,
                                totalVotes = totalVotes
                            )
                        else
                            null
                    )
                }

            val isChoiceChangeAllowed = source.permissionType == permissionTypeAllowChoiceChange

            return PollRecord(
                id = source.id,
                ownerAccountId = source.owner.id,
                subject = subject,
                choices = choices,
                currentChoice = null,
                isEnded = pollIsEnded,
                isChoiceChangeAllowed = isChoiceChangeAllowed,
                endDate = source.endTime
            )
        }
    }
}