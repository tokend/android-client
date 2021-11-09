package io.tokend.template.view.history

import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView

interface HistoryItemView {
    val iconImageView: AppCompatImageView
    val actionTextView: TextView
    val actionDetailsTextView: TextView
    val amountTextView: TextView
    val extraInfoTextView: TextView
    val dividerView: View

    val iconSize: Int

    val incomingColor: Int
    val outgoingColor: Int
    val defaultAmountColor: Int

    var dividerIsVisible: Boolean
}