package io.tokend.template.features.polls.view.adapter

import android.content.Context
import io.tokend.template.R
import io.tokend.template.features.polls.model.PollRecord
import io.tokend.template.view.util.LocalizedName
import io.tokend.template.view.util.RemainedTimeUtil

class PollListItem(
    val question: String,
    val choiceOptions: List<String>,
    val currentChoice: Int?,
    val outcomeByChoiceOption: List<OutcomeOfChoiceOption>?,
    val canVote: Boolean,
    val status: String,
    val source: PollRecord?
) {
    class OutcomeOfChoiceOption(
        val votesCount: Int,
        val percentOfTotal: Double
    )

    constructor(
        source: PollRecord,
        context: Context,
        localizedName: LocalizedName,
    ) : this(
        question = source.question,
        choiceOptions = source.choiceOptions,
        currentChoice = source.currentChoiceIndex,
        outcomeByChoiceOption =
        if (source.state is PollRecord.State.Closed) {
            val totalVotes = source.state.totalVotes

            source.state.outcomeByChoiceOption.map { votesCount ->
                OutcomeOfChoiceOption(
                    votesCount = votesCount,
                    percentOfTotal =
                    if (totalVotes == 0) {
                        0.0
                    } else {
                        (votesCount.toDouble() / totalVotes) * 100
                    }
                )
            }
        } else {
            null
        },
        canVote = source.canVote,
        status = when (source.state) {
            PollRecord.State.Upcoming -> {
                val (timeValue, timeUnit) = RemainedTimeUtil.getRemainedTime(source.startDate)
                context.getString(
                    R.string.template_time_to_start,
                    localizedName.forTimeWithUnit(timeUnit, timeValue)
                )
            }
            PollRecord.State.Active -> {
                val (timeValue, timeUnit) = RemainedTimeUtil.getRemainedTime(source.endDate)
                context.getString(
                    R.string.template_time_to_go,
                    localizedName.forTimeWithUnit(timeUnit, timeValue)
                )
            }
            PollRecord.State.Ended -> {
                context.getString(R.string.poll_ended)
            }
            is PollRecord.State.Closed -> {
                if (source.state.isPassed) {
                    context.getString(R.string.poll_passed)
                } else {
                    context.getString(R.string.poll_failed)
                }
            }
            PollRecord.State.Canceled -> {
                context.getString(R.string.poll_canceled)
            }
        },
        source = source
    )

    val canChoose = canVote && currentChoice == null
}