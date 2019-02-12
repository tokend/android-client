package org.tokend.template.view.history

import android.graphics.drawable.Drawable
import android.support.v7.widget.AppCompatImageView
import android.view.View
import android.widget.TextView

interface HistoryItemView {
    val iconImageView: AppCompatImageView
    val actionTextView: TextView
    val counterpartyTextView: TextView
    val amountTextView: TextView
    val extraInfoTextView: TextView
    val dividerView: View

    val incomingIcon: Drawable?
    val outgoingIcon: Drawable?
    val matchIcon: Drawable?
    val lockedIcon: Drawable?
    val unlockedIcon: Drawable?

    val iconSize: Int
    val iconSizeSmall: Int

    val receivedColor: Int
    val sentColor: Int
    val secondaryTextColor: Int

    var dividerIsVisible: Boolean
}