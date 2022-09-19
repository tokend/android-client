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
    val question: String,
    val choiceOptions: List<String>,
    /**
     * Index of choice from 0
     */
    var currentChoiceIndex: Int?,
    val isChoiceChangeAllowed: Boolean,
    val startDate: Date,
    val endDate: Date,
    val state: State,
) {
    private class ChoiceData
    @JsonCreator
    constructor(
        @JsonProperty("number")
        val number: Int,
        @JsonProperty("description")
        val description: String
    )

//    enum class SSState {
//        UPCOMING,
//        ACTIVE,
//        ENDED,
//        PASSED,
//        FAILED,
//        CANCELED
//    }

    sealed class State {
        // Start date hasn't come.
        object Upcoming : State()

        // Between start and end dates.
        object Active : State()

        // End date has come but the poll is not closed.
        object Ended : State()

        // The poll is closed successfully or not.
        class Closed(
            /**
             * Whether the state is [PollState.PASSED] or [PollState.FAILED]
             */
            val isPassed: Boolean,
            /**
             * Number of votes by choice option index.
             */
            val outcomeByChoiceOption: List<Int>,
        ) : State() {
            val totalVotes: Int = outcomeByChoiceOption.sum()
        }

        // The poll has been cancelled.
        object Canceled : State()
    }

    val canVote: Boolean
        get() = state == State.Active && (currentChoiceIndex == null || isChoiceChangeAllowed)

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

            val question = details.get("question").asText()

            val numberOfChoices = source.numberOfChoices

            val mapper = JsonApiTools.objectMapper
            val choiceOptions = details["choices"]
                .map { mapper.treeToValue(it, ChoiceData::class.java) }
                .sortedBy(ChoiceData::number)
                .also { choiceData ->
                    check(choiceData.size.toLong() == numberOfChoices) {
                        "Expected to have $numberOfChoices in details, but only there are only ${choiceData.size}\n" +
                                details
                    }

                    (1..numberOfChoices).forEach { choiceNumber ->
                        checkNotNull(choiceData.getOrNull(choiceNumber.toInt() - 1)) {
                            "Expected to have a choice option for number $choiceNumber\n" +
                                    details
                        }
                    }
                }
                .map(ChoiceData::description)

            val isChoiceChangeAllowed = source.permissionType == permissionTypeAllowChoiceChange

            val sourceState = PollState.fromValue(source.pollState.value)
            val now = Date()
            val state = when {
                // Preserve order.
                sourceState == PollState.OPEN && now.before(source.startTime) -> {
                    State.Upcoming
                }
                sourceState == PollState.OPEN && now.before(source.endTime) -> {
                    State.Active
                }
                sourceState == PollState.OPEN && (now == source.endTime || now.after(source.endTime)) -> {
                    State.Ended
                }
                sourceState == PollState.CANCELLED -> {
                    State.Canceled
                }
                sourceState == PollState.PASSED || sourceState == PollState.FAILED -> {
                    /**
                     * Outcome provided by the default poll closer service is the following:
                     * outcome: {2: 1, 3: 2}
                     * Key is a choice number, which is index+1
                     */
                    val outcomeByChoiceNumber = details["outcome"]
                    val outcomeByChoiceOption = (1..numberOfChoices).map { choiceOptionNumber ->
                        outcomeByChoiceNumber
                            .get(choiceOptionNumber.toString())
                            ?.intValue()
                            ?: 0
                    }

                    State.Closed(
                        isPassed = sourceState == PollState.PASSED,
                        outcomeByChoiceOption = outcomeByChoiceOption
                    )
                }
                else ->
                    throw IllegalStateException("Can't parse poll state for poll ${source.id}")
            }


            return PollRecord(
                id = source.id,
                ownerAccountId = source.owner.id,
                question = question,
                choiceOptions = choiceOptions,
                // Poll resource doesn't contain data on user's current choice.
                currentChoiceIndex = null,
                isChoiceChangeAllowed = isChoiceChangeAllowed,
                startDate = source.startTime,
                endDate = source.endTime,
                state = state
            )
        }
    }
}