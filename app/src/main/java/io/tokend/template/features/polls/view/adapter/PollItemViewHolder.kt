package io.tokend.template.features.polls.view.adapter

import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.tokend.template.R
import io.tokend.template.extensions.forEachChildWithIndex
import io.tokend.template.extensions.layoutInflater
import io.tokend.template.view.adapter.base.BaseViewHolder

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

    private val votesCountTopMargin =
        context.resources.getDimensionPixelSize(R.dimen.quarter_standard_margin)

    /**
     * @param actionListener receives current list item and selected choice
     * or null for choice removal
     */
    fun bindWithActionListener(
        item: PollListItem,
        actionListener: PollActionListener?
    ) {
        subjectTextView.text = item.question
        endedHintTextView.text =
            item.status//if (item.isEnded) pollEndedTitle else getTimeToGo(item.endDate)

        val themedHintTextContext = ContextThemeWrapper(context, R.style.HintText)

        choicesLayout.removeAllViews()
        item.choiceOptions.forEachIndexed { i, choiceOption ->
            val choiceView = context.layoutInflater
                .inflate(R.layout.list_item_poll_choice, choicesLayout, false)
                    as TextView
            choicesLayout.addView(choiceView)

            choiceView.text = choiceOption
            choiceView.tag = i

            if (item.canChoose) {
                choiceView.setOnClickListener { onNewChoice(i, item, actionListener) }
            }

            if (item.outcomeByChoiceOption != null) {
                val outcome = item.outcomeByChoiceOption[i]
                val votesCountTextView = TextView(themedHintTextContext, null, R.style.HintText)
                votesCountTextView.text = context.resources.getQuantityString(
                    R.plurals.vote_with_percent,
                    outcome.votesCount,
                    outcome.votesCount,
                    outcome.percentOfTotal
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

    private fun onNewChoice(
        currentChoice: Int,
        item: PollListItem,
        actionListener: (PollActionListener)?
    ) {
        updateChoicesBackground(currentChoice, item)
        updateActionButtonState(currentChoice, item, actionListener)
    }

    private fun updateChoicesBackground(
        currentChoice: Int?,
        item: PollListItem
    ) {
        choicesLayout.forEachChildWithIndex { _, choiceView ->
            val choiceIndex = choiceView.tag as? Int
                ?: return@forEachChildWithIndex

            val backgroundDrawable =
                (if (choiceIndex == currentChoice)
                    ContextCompat.getDrawable(context, R.drawable.poll_choice_selected_background)
                else
                    ContextCompat.getDrawable(context, R.drawable.poll_choice_background))
                    ?.mutate()

            val outcome = item.outcomeByChoiceOption?.get(choiceIndex)
            backgroundDrawable?.level = 100 * (outcome?.percentOfTotal?.toInt() ?: 0)

            choiceView.background = backgroundDrawable
        }
    }

    private fun updateActionButtonState(
        currentChoice: Int?,
        item: PollListItem,
        actionListener: (PollActionListener)?
    ) {
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
}