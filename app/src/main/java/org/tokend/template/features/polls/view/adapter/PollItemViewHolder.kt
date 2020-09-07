package org.tokend.template.features.polls.view.adapter

import androidx.core.content.ContextCompat
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.jetbrains.anko.forEachChildWithIndex
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.LocalizedName
import org.tokend.template.view.util.RemainedTimeUtil
import java.util.*
import kotlin.math.absoluteValue

class PollItemViewHolder(view: View) : BaseViewHolder<PollListItem>(view) {
    private val context = view.context
    private val subjectTextView: TextView = view.findViewById(R.id.subject_text_view)
    private val endedHintTextView: TextView = view.findViewById(R.id.ended_hint_text_view)
    private val choicesLayout: ViewGroup = view.findViewById(R.id.choices_layout)
    private val actionButton: TextView = view.findViewById(R.id.vote_button)
    private val actionButtonPlaceholder: View = view.findViewById(R.id.button_placeholder)

    private val actionButtonVoteTitle =
            context.getString(R.string.vote_action)
    private val actionButtonRemoveVoteTitle =
            context.getString(R.string.remove_vote_action)
    private val pollEndedTitle =
            context.getString(R.string.poll_ended)

    private val votesCountTopMargin =
            context.resources.getDimensionPixelSize(R.dimen.quarter_standard_margin)

    private val localizedName = LocalizedName(view.context)

    /**
     * @param actionListener receives current list item and selected choice
     * or null for choice removal
     */
    fun bindWithActionListener(item: PollListItem,
                               actionListener: PollActionListener?) {
        subjectTextView.text = item.subject
        endedHintTextView.text = if (item.isEnded) pollEndedTitle else getTimeToGo(item.endDate)

        val themedHintTextContext = ContextThemeWrapper(context, R.style.HintText)

        choicesLayout.removeAllViews()
        item.choices.forEachIndexed { i, choice ->
            val choiceView = context.layoutInflater
                    .inflate(R.layout.list_item_poll_choice, choicesLayout, false)
                    as TextView
            choicesLayout.addView(choiceView)

            choiceView.text = choice.name
            choiceView.tag = i

            if (item.canChoose) {
                choiceView.onClick { onNewChoice(i, item, actionListener) }
            }

            val resultData = choice.result
            if (resultData != null) {
                val votesCountTextView = TextView(themedHintTextContext, null, R.style.HintText)
                votesCountTextView.text = context.resources.getQuantityString(
                        R.plurals.vote_with_percent,
                        resultData.votesCount.absoluteValue,
                        resultData.votesCount,
                        resultData.percentOfTotal
                )
                val layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
                layoutParams.topMargin = votesCountTopMargin
                votesCountTextView.layoutParams = layoutParams
                choicesLayout.addView(votesCountTextView)
            }
        }

        updateActionButtonState(item.currentChoice, item, actionListener)
        updateChoicesBackground(item.currentChoice, item)
    }

    override fun bind(item: PollListItem) = bindWithActionListener(item, null)

    private fun onNewChoice(currentChoice: Int,
                            item: PollListItem,
                            actionListener: (PollActionListener)?) {
        updateChoicesBackground(currentChoice, item)
        updateActionButtonState(currentChoice, item, actionListener)
    }

    private fun updateChoicesBackground(currentChoice: Int?,
                                        item: PollListItem) {
        choicesLayout.forEachChildWithIndex { _, choiceView ->
            val choiceIndex = choiceView.tag as? Int
                    ?: return@forEachChildWithIndex

            val backgroundDrawable =
                    (if (choiceIndex == currentChoice)
                        ContextCompat.getDrawable(context, R.drawable.poll_choice_selected_background)
                    else
                        ContextCompat.getDrawable(context, R.drawable.poll_choice_background))
                            ?.mutate()

            val resultData = item.choices.getOrNull(choiceIndex)?.result
            backgroundDrawable?.level =
                    if (resultData != null)
                        (resultData.percentOfTotal * 100).toInt()
                    else
                        0

            choiceView.background = backgroundDrawable
        }
    }

    private fun updateActionButtonState(currentChoice: Int?,
                                        item: PollListItem,
                                        actionListener: (PollActionListener)?) {
        if (!item.canVote) {
            actionButton.visibility = View.GONE
            actionButtonPlaceholder.visibility = View.VISIBLE
            return
        }

        actionButton.visibility = View.VISIBLE
        actionButtonPlaceholder.visibility = View.GONE

        val hasChoice = currentChoice != null

        if (!hasChoice) {
            actionButton.text = actionButtonVoteTitle
            actionButton.isEnabled = false
        } else {
            actionButton.isEnabled = true

            if (item.canChoose) {
                actionButton.text = actionButtonVoteTitle
                if (actionListener != null) {
                    actionButton.setOnClickListener { actionListener.invoke(item, currentChoice) }
                }
            } else {
                actionButton.text = actionButtonRemoveVoteTitle
                if (actionListener != null) {
                    actionButton.setOnClickListener { actionListener.invoke(item, null) }
                }
            }
        }
    }

    private fun getTimeToGo(endDate: Date): String {
        val (timeValue, timeUnit) = RemainedTimeUtil.getRemainedTime(endDate)

        return context.getString(
                R.string.template_days_to_go,
                timeValue,
                localizedName.forTimeUnit(timeUnit, timeValue)
        )
    }
}