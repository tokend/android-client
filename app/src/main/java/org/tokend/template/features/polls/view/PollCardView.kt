package org.tokend.template.features.polls.view

import android.content.Context
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.features.polls.model.PollRecord

class PollCardView
@JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val subject: TextView
    private val choicesLayout: LinearLayout
    private val voteButton: Button

    var voteEnabled: Boolean = true
        set(value) {
            field = value
            voteButton.isEnabled = value
        }

    private var selectedItemIndex: Int = -1
        set(value) {
            field = value
            highlightItem()
        }

    var isVote: Boolean = false
    set(value) {
        field = value
        val text = if (value) {
           R.string.unvote_title
        } else {
            R.string.vote_title
        }
        setButtonText(text)
    }

    private var onVoteListener: ((selectedIndex: Int, vote: Boolean) -> Unit)? = null

    init {
        context.layoutInflater.inflate(R.layout.layout_poll_view,
                this, true)

        subject = findViewById(R.id.subject_text_view)
        choicesLayout = findViewById(R.id.choices_layout)
        voteButton = findViewById(R.id.vote_button)

        voteButton.onClick {
            isVote = !isVote
            onVoteListener?.invoke(selectedItemIndex, isVote)
        }
    }

    fun displayPoll(poll: PollRecord, onVote: (selectedIndex: Int, vote: Boolean) -> Unit) {
        displaySubject(poll.subject)
        displayChoices(poll.choices)

        poll.currentChoice?.let {
            selectItem(it)
            isVote = true
        }

        onVoteListener = onVote
    }

    private fun displaySubject(subject: String) {
        this.subject.text = subject
    }

    private fun displayChoices(choices: List<String>) {
        choicesLayout.removeAllViews()

        for (i in choices.indices) {
            val view = context.layoutInflater
                    .inflate(R.layout.list_item_poll_choice, this, false) as TextView

            view.text = choices[i]
            choicesLayout.addView(view)

            view.onClick {
                if (!voteEnabled) return@onClick
                selectItem(i)
            }
        }
    }

    private fun selectItem(index: Int) {
        if (selectedItemIndex == index || isVote) return
        deselectPreviousItem()
        selectedItemIndex = index
    }

    private fun deselectPreviousItem() {
        if (selectedItemIndex == -1) return
        (choicesLayout.getChildAt(selectedItemIndex) as TextView).apply {
            background = ContextCompat
                    .getDrawable(context, R.drawable.rounded_stroke_card_background)
        }
    }

    private fun highlightItem() {
        (choicesLayout.getChildAt(selectedItemIndex) as TextView).apply {
            background = ContextCompat
                    .getDrawable(context, R.drawable.rounded_stroke_card_background_selected)
        }
    }

    private fun setButtonText(@StringRes text: Int) {
        voteButton.text = context.getString(text)
    }
}