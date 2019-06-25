package org.tokend.template.features.polls.view.adapter

import android.support.v4.content.ContextCompat
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

class PollItemViewHolder(view: View) : BaseViewHolder<PollListItem>(view) {
    private val context = view.context
    private val subjectTextView: TextView = view.findViewById(R.id.subject_text_view)
    private val endedHintTextView: TextView = view.findViewById(R.id.ended_hint_text_view)
    private val choicesLayout: ViewGroup = view.findViewById(R.id.choices_layout)
    private val actionButton: TextView = view.findViewById(R.id.vote_button)
    private val actionButtonPlaceholder: View = view.findViewById(R.id.button_placeholder)

    private val choiceDefaultBackground =
            ContextCompat.getDrawable(context, R.drawable.poll_choice_background)
    private val choiceSelectedBackground =
            ContextCompat.getDrawable(context, R.drawable.poll_choice_selected_background)

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
    fun bindWithActionListener(item: PollListItem,
                               actionListener: PollActionListener?) {
        subjectTextView.text = item.subject
        endedHintTextView.visibility = if (item.isEnded) View.VISIBLE else View.GONE

        val themedHintTextContext = ContextThemeWrapper(context, R.style.HintText)

        choicesLayout.removeAllViews()
        item.choices.forEachIndexed { i, choice ->
            val choiceView = context.layoutInflater
                    .inflate(R.layout.list_item_poll_choice, choicesLayout, false)
                    as TextView
            choicesLayout.addView(choiceView)

            choiceView.text = choice.name
            choiceView.tag = i

            if (item.canVote) {
                choiceView.onClick { onNewChoice(i, item, actionListener) }
            }

            val resultData = choice.result
            if (resultData != null) {
                val votesCountTextView = TextView(themedHintTextContext, null, R.style.HintText)
                votesCountTextView.text = context.resources.getQuantityString(
                        R.plurals.vote_with_percent,
                        resultData.votesCount,
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
        choicesLayout.forEachChildWithIndex { i, choiceView ->
            val choiceIndex = choiceView.tag as? Int
                    ?: return@forEachChildWithIndex

            choiceView.background =
                    if (choiceIndex == currentChoice)
                        choiceSelectedBackground
                    else
                        choiceDefaultBackground

            val resultData = item.choices.getOrNull(choiceIndex)?.result
            choiceView.background.level =
                    if (resultData != null)
                        (resultData.percentOfTotal * 100).toInt()
                    else
                        0
        }
    }

    private fun updateActionButtonState(currentChoice: Int?,
                                        item: PollListItem,
                                        actionListener: (PollActionListener)?) {
        if (item.hasResults) {
            actionButton.visibility = View.GONE
            actionButtonPlaceholder.visibility = View.VISIBLE
            return
        }

        actionButton.visibility = View.VISIBLE
        actionButtonPlaceholder.visibility = View.GONE

        if (currentChoice == null) {
            actionButton.text = actionButtonVoteTitle
            actionButton.isEnabled = false
        } else {
            actionButton.text =
                    if (item.canVote)
                        actionButtonVoteTitle
                    else
                        actionButtonRemoveVoteTitle

            actionButton.isEnabled = true

            if (actionListener != null) {
                actionButton.setOnClickListener {
                    if (item.canVote) {
                        actionListener.invoke(item, currentChoice)
                    } else {
                        actionListener.invoke(item, null)
                    }
                }
            }
        }
    }
}