package org.tokend.template.features.polls.view.adapter

import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.anko.forEachChildWithIndex
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder

class PollItemViewHolder(view: View) : BaseViewHolder<PollListItem>(view) {
    private val context = view.context
    private val subjectTextView: TextView = view.findViewById(R.id.subject_text_view)
    private val choicesLayout: ViewGroup = view.findViewById(R.id.choices_layout)
    private val actionButton: TextView = view.findViewById(R.id.vote_button)

    private val choiceDefaultBackground =
            ContextCompat.getDrawable(context, R.drawable.rounded_stroke_card_background)
    private val choiceSelectedBackground =
            ContextCompat.getDrawable(context, R.drawable.rounded_stroke_card_background_selected)

    private val actionButtonVoteTitle =
            context.getString(R.string.vote_action)
    private val actionButtonRemoveVoteTitle =
            context.getString(R.string.remove_vote_action)

    /**
     * @param actionListener receives current list item and selected choice
     * or null for choice removal
     */
    fun bindWithActionListener(item: PollListItem,
                               actionListener: PollActionListener?) {
        val canChoose = item.currentChoice == null

        subjectTextView.text = item.subject

        choicesLayout.removeAllViews()
        item.choices.forEachIndexed { i, choice ->
            val choiceView = context.layoutInflater
                    .inflate(R.layout.list_item_poll_choice, choicesLayout, false)
                    as TextView
            choicesLayout.addView(choiceView)

            choiceView.text = choice

            if (canChoose) {
                choiceView.onClick { onNewChoice(i, item, actionListener) }
            }
        }

        updateActionButtonState(item.currentChoice, item, actionListener)
        updateChoicesBackground(item.currentChoice)
    }

    override fun bind(item: PollListItem) = bindWithActionListener(item, null)

    private fun onNewChoice(currentChoice: Int,
                            item: PollListItem,
                            actionListener: (PollActionListener)?) {
        updateChoicesBackground(currentChoice)
        updateActionButtonState(currentChoice, item, actionListener)
    }

    private fun updateChoicesBackground(currentChoice: Int?) {
        choicesLayout.forEachChildWithIndex { i, choiceView ->
            choiceView.background =
                    if (i == currentChoice)
                        choiceSelectedBackground
                    else
                        choiceDefaultBackground

        }
    }

    private fun updateActionButtonState(currentChoice: Int?,
                                        item: PollListItem,
                                        actionListener: (PollActionListener)?) {
        if (currentChoice == null) {
            actionButton.text = actionButtonVoteTitle
            actionButton.isEnabled = false
        } else {
            val canChoose = item.currentChoice == null

            actionButton.text =
                    if (canChoose)
                        actionButtonVoteTitle
                    else
                        actionButtonRemoveVoteTitle

            actionButton.isEnabled = true

            if (actionListener != null) {
                actionButton.setOnClickListener {
                    if (canChoose) {
                        actionListener.invoke(item, currentChoice)
                    } else {
                        actionListener.invoke(item, null)
                    }
                }
            }
        }
    }
}