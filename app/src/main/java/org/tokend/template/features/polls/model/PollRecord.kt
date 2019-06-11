package org.tokend.template.features.polls.model

import org.tokend.sdk.api.generated.resources.PollResource
import kotlin.random.Random

class PollRecord(
        val id: String,
        val ownerAccountId: String,
        val subject: String,
        val choices: List<Choice>,
        var currentChoice: Int?
) {
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

            val subject = details.get("subject").asText()

            // TODO: Resolve
            val choicesNames = (1..source.numberOfChoices).map { "Choice #$it" }
            val votesCounts = (1..source.numberOfChoices)
                    .map { (Random.nextInt() and 0xffff).toLong() }
            val totalVotes = votesCounts.sum()

            val choices = (0 until source.numberOfChoices)
                    .map { it.toInt() }
                    .map {
                        val votesCount = votesCounts[it].toInt()
                        Choice(
                                choicesNames[it],
                                Choice.Result(
                                        votesCount = votesCount,
                                        totalVotes = totalVotes
                                )
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