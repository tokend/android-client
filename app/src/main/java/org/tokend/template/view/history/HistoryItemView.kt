package org.tokend.template.view.history

import android.support.v7.widget.AppCompatImageView
import android.view.View
import android.widget.TextView

interface HistoryItemView {
    val iconImageView: AppCompatImageView
    val actionTextView: TextView
    val actionDetailsTextView: TextView
    val amountTextView: TextView
    val extraInfoTextView: TextView
    val dividerView: View

    val iconSize: Int
    val iconSizeSmall: Int

    val incomingColor: Int
    val outgoingColor: Int
    val defaultAmountColor: Int

    var dividerIsVisible: Boolean
}