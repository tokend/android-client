package org.tokend.template.features.history.view

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import org.tokend.template.R
import org.tokend.template.features.history.model.BalanceChange
import org.tokend.template.features.history.model.BalanceChangeAction
import org.tokend.template.features.history.view.adapter.BalanceChangeListItem

class BalanceChangeIconFactory(context: Context) {
    private val incomingIcon: Drawable? =
            ContextCompat.getDrawable(context, R.drawable.ic_tx_received)

    private val outgoingIcon: Drawable? =
            ContextCompat.getDrawable(context, R.drawable.ic_tx_sent)

    private val matchIcon: Drawable? =
            ContextCompat.getDrawable(context, R.drawable.ic_tx_match)

    private val lockedIcon: Drawable? =
            ContextCompat.getDrawable(context, R.drawable.ic_tx_locked)

    private val unlockedIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_tx_unlocked)

    fun get(action: BalanceChangeListItem.Action,
            isReceived: Boolean?): Drawable? {
        return when (action) {
            BalanceChangeListItem.Action.LOCKED -> lockedIcon
            BalanceChangeListItem.Action.UNLOCKED -> unlockedIcon
            BalanceChangeListItem.Action.MATCHED -> matchIcon
            else -> when (isReceived) {
                true -> incomingIcon
                false -> outgoingIcon
                null -> matchIcon
            }
        }
    }

    fun get(item: BalanceChange): Drawable? {
        return when (item.action) {
            BalanceChangeAction.LOCKED -> lockedIcon
            BalanceChangeAction.UNLOCKED -> unlockedIcon
            BalanceChangeAction.MATCHED -> matchIcon
            else -> when (item.isReceived) {
                true -> incomingIcon
                false -> outgoingIcon
                null -> matchIcon
            }
        }
    }
}